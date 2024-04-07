package com.nfyc.lcnotificationservice.service;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableServiceException;
import com.azure.data.tables.models.TableTransactionAction;
import com.azure.data.tables.models.TableTransactionActionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nfyc.lcnotificationservice.domain.LcQuestionPriority;
import com.nfyc.lcnotificationservice.domain.NfycLcResponse;
import com.nfyc.lcnotificationservice.domain.NfycLcUser;
import com.nfyc.lcnotificationservice.domain.NfycLcUserDailyStatusChallenge;
import com.nfyc.lcnotificationservice.utils.NfycGraphQLQueries;
import com.nfyc.lcnotificationservice.utils.NfycLcEcxeption;
import com.nfyc.lcnotificationservice.utils.NfycLcError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.client.ClientResponseField;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.nfyc.lcnotificationservice.utils.NfycLcConstants.RECORD_LIMIT;

@Slf4j
@RequiredArgsConstructor
@Service
public class NfycLeetcodeService {
  //
  private final HttpGraphQlClient nfycGraphQLClient;
  private final ObjectMapper objectMapper;
  private final TableClient tableClient;
  private final Validator validator;
  private final NfycRevisionAlgo nfycRevisionAlgo;
  private final NfycAzureTableQueryUtil nfycAzureTableQueryUtil;

  private String dailyChallengeTitleSlug = "";
  private Date dailyChallengeQuesDate = null;

  public NfycLcResponse getDailyChallenge() {
    return this.nfycGraphQLClient.document(NfycGraphQLQueries.GET_DAILY_CONTEST_INFO)
        .retrieve("activeDailyCodingChallengeQuestion").toEntity(JsonNode.class)
        .map(NfycLcResponse::new).block();
  }

  public NfycLcResponse getQuestionDetails(String titleSlug) {
    return this.nfycGraphQLClient.document(NfycGraphQLQueries.GET_QUESTION_DETAIL)
        .variable("titleSlug", titleSlug)
        .execute().<JsonNode>handle((response, sink) -> {
          if (!response.isValid()) {
            sink.error(new NfycLcEcxeption(NfycLcError.ERROR_LC_API_REQUEST_FAILED));
            return;
          }
          ClientResponseField question = response.field("question");
          if (question.getValue() == null) {
            log.info("The Question: " + titleSlug + " is invalid");
            sink.error(new NfycLcEcxeption(NfycLcError.ERROR_QUESTION_INFO_NOT_FOUND, titleSlug));
          }
          ;
          sink.next(response.toEntity(JsonNode.class));
        })
        .map(NfycLcResponse::new)
        .block();
  }

  public NfycLcResponse saveLcUser(NfycLcUser nfycLcUser) {
    try {
      Set<ConstraintViolation<NfycLcUser>> violations = validator.validate(nfycLcUser);
      if (!violations.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        for (ConstraintViolation<NfycLcUser> constraintViolation : violations) {
          sb.append(constraintViolation.getMessage());
        }
        throw new ConstraintViolationException("Error occurred: " + sb.toString(), violations);
      }
      if (this.doesUserAlreadyExists(nfycLcUser)) {
        log.info("Cannot create user as user already exists");
        throw new NfycLcEcxeption(NfycLcError.ERROR_USER_EMAIL_ALREADY_EXIST, nfycLcUser.getEmail());
      }
      NfycLcResponse nfycLcResponse = null;
      try {
        nfycLcResponse = this.getRecentACSubmissionForAUser(nfycLcUser.getLcUsername());
      } catch (NfycLcEcxeption e) {
        throw new NfycLcEcxeption(NfycLcError.ERROR_LC_USERNAME_INVALID, nfycLcUser.getLcUsername());
      }
      tableClient.upsertEntity(convertNfycLcUserToTableEntity(nfycLcUser));
      this.updateUserRecentlyACQuestions(nfycLcUser, nfycLcResponse);
      ObjectNode response = objectMapper.createObjectNode();
      response.put("message", "User was successfully created");
      return new NfycLcResponse(response);
    } catch (ConstraintViolationException e) {
      log.error("Validation Error Occurred while creating user: " + e.getConstraintViolations());
      throw new NfycLcEcxeption(NfycLcError.ERROR_USER_CREATION_FAILED, e.getConstraintViolations().stream()
          .findFirst().get().getMessage());
    }
  }

  public String fetchLatestACQuestionsForUsers() {
    String userPartitionKey = "nfyclcuser";
    ListEntitiesOptions options = new ListEntitiesOptions()
        .setFilter(this.nfycAzureTableQueryUtil.getAllEntitiesByPartitionKey(userPartitionKey));
    List<NfycLcUser> user = tableClient.listEntities(options, null, null)
        .stream().map(this::convertTableEntityToNfycLcUser).toList();

    user.parallelStream().flatMap(currentUser -> {
      NfycLcResponse userSubmission = getRecentACSubmissionForAUser(currentUser.getLcUsername());
      ArrayNode acArray = (ArrayNode) userSubmission.getData().get("recentAcSubmissionList");
      for (JsonNode question : acArray) {
        ObjectNode objectNode = (ObjectNode) question;
        objectNode.put("email", currentUser.getEmail());
      }
      return StreamSupport.stream(acArray.spliterator(), true);
    }).filter(question -> {
      String timestamp = question.get("timestamp").asText();
      Date date = this.getDateFromTimestamp(timestamp);
      return this.nfycRevisionAlgo.isPreviousDay(date);
    }).map(question -> {
      String titleSlug = question.get("titleSlug").asText();
      String timestamp = question.get("timestamp").asText();
      String title = question.get("title").asText();

      String email = question.get("email").asText();
      String rowKey = email + ":" + titleSlug;

      try {
        Map<String, Object> nfycLcUserQuestion = new HashMap<>();
        nfycLcUserQuestion.put("questionTitleSlug", titleSlug);
        nfycLcUserQuestion.put("questionTitle", title);
        nfycLcUserQuestion.put("lastSolvedDate", this.getDateFromTimestamp(timestamp));
        nfycLcUserQuestion.put("lastRevisedDate", new Date());
        Map<Boolean, TableEntity> isQuestionAlreadyAttempted = this.didUserHasResubmittedAQuestion(rowKey);
        if (isQuestionAlreadyAttempted.containsKey(true)) {
          int revisionCount = (int) isQuestionAlreadyAttempted.get(true).getProperty("revisionCount") + 1;
          nfycLcUserQuestion.put("revisionCount", revisionCount);
          nfycLcUserQuestion.put("priority", isQuestionAlreadyAttempted.get(true).getProperty("priority"));
        } else {
          nfycLcUserQuestion.put("revisionCount", 1);
          NfycLcResponse questionDetail = getQuestionDetails(titleSlug);
          int priority = LcQuestionPriority.valueOf(questionDetail.getData().get("question")
              .get("difficulty").asText()).getPriority();
          nfycLcUserQuestion.put("priority", priority);
        }
        nfycLcUserQuestion.put("nextRevisionDate", this.nfycRevisionAlgo.getNextRevisionDate(new Date(), (int) nfycLcUserQuestion.get("priority"),
            (int) nfycLcUserQuestion.get("revisionCount")));
        return new TableTransactionAction(
            TableTransactionActionType.UPSERT_REPLACE,
            new TableEntity("ac_questions", rowKey)
                .setProperties(nfycLcUserQuestion));
      } catch (Exception e) {
        log.error("Exception Occurred while fetching question details " + e.getMessage());
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.collectingAndThen(Collectors.toList(),
        tableClient::submitTransaction));
    return "Completed";
  }

  private NfycLcUser convertTableEntityToNfycLcUser(TableEntity tableEntity) {
    return NfycLcUser.builder()
        .userId(UUID.fromString(tableEntity.getProperty("userId").toString()))
        .email(tableEntity.getRowKey())
        .lcUsername(tableEntity.getProperty("lcUsername").toString()).build();
  }

  private void updateUserRecentlyACQuestions(NfycLcUser nfycLcUser, NfycLcResponse userSubmission) {
    String partitionKey = "ac_questions";
    ArrayNode acArray = (ArrayNode) userSubmission.getData().get("recentAcSubmissionList");
    StreamSupport.stream(acArray.spliterator(), true).map(question -> {
      String titleSlug = question.get("titleSlug").asText();
      String timestamp = question.get("timestamp").asText();
      String title = question.get("title").asText();
      try {
        NfycLcResponse questionDetail = getQuestionDetails(titleSlug);
        int priority = LcQuestionPriority.valueOf(questionDetail.getData().get("question")
            .get("difficulty").asText()).getPriority();
        Map<String, Object> nfycLcUserQuestion = new HashMap<>();
        nfycLcUserQuestion.put("questionTitle", title);
        nfycLcUserQuestion.put("questionTitleSlug", titleSlug);
        nfycLcUserQuestion.put("lastSolvedDate", this.getDateFromTimestamp(timestamp));
        nfycLcUserQuestion.put("lastRevisedDate", new Date());
        nfycLcUserQuestion.put("revisionCount", 1);
        nfycLcUserQuestion.put("priority", priority);
        nfycLcUserQuestion.put("nextRevisionDate", this.nfycRevisionAlgo.getNextRevisionDate(new Date(), priority, 1));
        String rowKey = nfycLcUser.getEmail() + ":" + titleSlug;
        return new TableTransactionAction(
            TableTransactionActionType.CREATE,
            new TableEntity(partitionKey, rowKey)
                .setProperties(nfycLcUserQuestion));
      } catch (Exception e) {
        log.error("Exception Occurred while fetching question details " + e.getMessage());
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.collectingAndThen(Collectors.toList(),
        tableClient::submitTransaction));
  }

  private TableEntity convertNfycLcUserToTableEntity(NfycLcUser nfycLcUser) {
    String partitionKey = "nfyclcuser";
    String userId = UUID.randomUUID().toString();
    Map<String, Object> nfycLcUserProperty = new HashMap<>();
    nfycLcUserProperty.put("userId", userId);
    nfycLcUserProperty.put("email", nfycLcUser.getEmail());
    nfycLcUserProperty.put("lcUsername", nfycLcUser.getLcUsername());
    return new TableEntity(partitionKey, nfycLcUser.getEmail()).setProperties(nfycLcUserProperty);
  }

  private Map<Boolean, TableEntity> didUserHasResubmittedAQuestion(String key) {
    Map<Boolean, TableEntity> result = new HashMap<>(1);
    String partitionKey = "ac_questions";
    try {
      TableEntity tableEntity = tableClient.getEntity(partitionKey, key);
      result.put(true, tableEntity);
    } catch (TableServiceException tableServiceException) {
      result.put(false, null);
      ;
    }
    return result;
  }

  private boolean doesUserAlreadyExists(NfycLcUser nfycLcUser) {
    String partitionKey = "nfyclcuser";
    try {
      TableEntity tableEntity = tableClient.getEntity(partitionKey, nfycLcUser.getEmail());
    } catch (TableServiceException tableServiceException) {
      return false;
    }
    return true;
  }

  public NfycLcResponse getRecentACSubmissionForAUser(String lcUsername) {
    return this.nfycGraphQLClient.document(NfycGraphQLQueries.GET_RECENT_AC_SUBMISSION)
        .variable("username", lcUsername)
        .variable("limit", RECORD_LIMIT)
        .execute()
        .<JsonNode>handle((response, sink) -> {
          if (!response.isValid()) {
            sink.error(new NfycLcEcxeption(NfycLcError.ERROR_LC_API_REQUEST_FAILED));
            return;
          }
          List<ResponseError> error = response.getErrors();
          if (!error.isEmpty()) {
            ClientResponseField username = response.field("matchedUser");
            if (username.getValue() == null) {
              log.info("The username: " + lcUsername + " is invalid");
              sink.error(new NfycLcEcxeption(NfycLcError.ERROR_LC_USERNAME_INVALID, lcUsername));
            } else {
              log.error("Error occurred while hitting lc api: getRecentSubmissionForAUser");
              sink.error(new NfycLcEcxeption(NfycLcError.ERROR_LC_API_REQUEST_FAILED));
            }
            return;
          }
          sink.next(response.toEntity(JsonNode.class));
        })
        .map(NfycLcResponse::new)
        .block();
  }

  public NfycLcUserDailyStatusChallenge hasUserSuccessfullySubmittedTheDailyChallenge(NfycLcUser nfycLcUser) {
    try {
      NfycLcResponse userSubmission = getRecentACSubmissionForAUser(nfycLcUser.getLcUsername());
      ArrayNode acArray = (ArrayNode) userSubmission.getData().get("recentAcSubmissionList");
      for (JsonNode question : acArray) {
        String titleSlug = question.get("titleSlug").asText();
        String timestamp = question.get("timestamp").asText();
        if (titleSlug.equals(dailyChallengeTitleSlug) && isSubmissionDateValid(timestamp)) {
          return NfycLcUserDailyStatusChallenge.SUBMITTED;
        }
      }
    } catch (Exception e) {
      return NfycLcUserDailyStatusChallenge.ERROR;
    }
    return NfycLcUserDailyStatusChallenge.NOT_SUBMITTED;
  }

  private Date getDateFromTimestamp(String timestampStr) {
    long timestamp = Long.parseLong(timestampStr);
    return (Date.from(Instant.ofEpochSecond(timestamp)));
  }

  private boolean isSubmissionDateValid(String timestampStr) {
    Date date = getDateFromTimestamp(timestampStr);
    return date.after(dailyChallengeQuesDate);
  }

  public void setDailyChallengeInfo() {
    if (dailyChallengeTitleSlug.isEmpty()) {
      NfycLcResponse activeDailyChallengeReq = getDailyChallenge();
      System.out.println(activeDailyChallengeReq.getData().toString());
      dailyChallengeTitleSlug = activeDailyChallengeReq.getData()
          .get("question").get("titleSlug").asText();
      String date = activeDailyChallengeReq.getData().get("date").asText();
      if (Objects.isNull(dailyChallengeTitleSlug)) {
        log.error("Error while fetching the daily challenge info");
        throw new NfycLcEcxeption(NfycLcError.ERROR_LC_API_REQUEST_FAILED);
      }
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      try {
        dailyChallengeQuesDate = sdf.parse(date);
      } catch (ParseException pe) {
        log.error("Could not correctly parse date while fetching the daily challenge info");
        throw new NfycLcEcxeption(NfycLcError.ERROR_LC_API_REQUEST_FAILED);
      }
    }
  }
}
