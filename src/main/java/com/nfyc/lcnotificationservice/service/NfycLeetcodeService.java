package com.nfyc.lcnotificationservice.service;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nfyc.lcnotificationservice.domain.NfycLcResponse;
import com.nfyc.lcnotificationservice.domain.NfycLcUser;
import com.nfyc.lcnotificationservice.domain.NfycLcUserDailyStatusChallenge;
import com.nfyc.lcnotificationservice.utils.NfycGraphQLQueries;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.nfyc.lcnotificationservice.utils.NfycLcConstants.RECORD_LIMIT;

@Slf4j
@RequiredArgsConstructor
@Service
public class NfycLeetcodeService {

  private final HttpGraphQlClient nfycGraphQLClient;
  private final ObjectMapper objectMapper;
  private final TableClient tableClient;
  private final Validator validator;
  private String dailyChallengeTitleSlug = "";
  private Date dailyChallengeQuesDate = null;

  public NfycLcResponse getDailyChallenge() {
    return this.nfycGraphQLClient.document(NfycGraphQLQueries.GET_DAILY_CONTEST_INFO)
        .retrieve("activeDailyCodingChallengeQuestion").toEntity(JsonNode.class)
        .map(NfycLcResponse::new).block();
  }

  public NfycLcResponse saveLcUser(NfycLcUser nfycLcUser) {
    try {
      if (this.doesUserAlreadyExists(nfycLcUser)) {
        log.info("Cannot create user as user already exists");
        throw new RuntimeException("User Cannot be created");
      }
      Set<ConstraintViolation<NfycLcUser>> violations = validator.validate(nfycLcUser);
      if (!violations.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        for (ConstraintViolation<NfycLcUser> constraintViolation : violations) {
          sb.append(constraintViolation.getMessage());
        }
        throw new ConstraintViolationException("Error occurred: " + sb.toString(), violations);
      }
      tableClient.upsertEntity(convertNfycLcUserToTableEntity(nfycLcUser));
      ObjectNode response = objectMapper.createObjectNode();
      response.put("message", "User was successfully created");
      return new NfycLcResponse(response);
    } catch (ConstraintViolationException e) {
//      throw new RuntimeException(e.getMessage());
      throw e;
    }
  }

  private TableEntity convertNfycLcUserToTableEntity(NfycLcUser nfycLcUser) {
    String partitionKey = "nfyclcuser";
    String userId = UUID.randomUUID().toString();
    Map<String, Object> nfycLcUserProperty = new HashMap<>();
    nfycLcUserProperty.put("userId", userId);
    nfycLcUserProperty.put("email", nfycLcUser.getEmail());
    nfycLcUserProperty.put("fullName", nfycLcUser.getFullName());
    nfycLcUserProperty.put("lcUsername", nfycLcUser.getLcUsername());
    return new TableEntity(partitionKey, nfycLcUser.getEmail()).setProperties(nfycLcUserProperty);
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
            sink.error(new RuntimeException("Request failed"));
            return;
          }
          System.out.println("Response is " + response);
          List<ResponseError> error = response.getErrors();
          if (!error.isEmpty()) {
            ClientResponseField username = response.field("matchedUser");
            if (username.getValue() == null) {
              System.out.println("The username: " + lcUsername + " is invalid");
              sink.error(new RuntimeException("The username is invalid"));
            } else {
              System.out.println("The some error occrured");
              sink.error(new RuntimeException("Some error occureed"));
            }
            return;
          }
          sink.next(response.toEntity(JsonNode.class));
        })
        .map(NfycLcResponse::new)
        .onErrorMap(error -> new RuntimeException(error.getMessage()))
        .block();
  }

  public NfycLcUserDailyStatusChallenge hasUserSuccessfullySubmittedTheDailyChallenge(NfycLcUser nfycLcUser) {
    try {
      NfycLcResponse userSubmission = getRecentACSubmissionForAUser(nfycLcUser.getLcUsername());
      ArrayNode acArray = (ArrayNode) userSubmission.getData().get("recentAcSubmissionList");
      for (JsonNode question : acArray) {
        String titleSlug = question.get("titleSlug").asText();
        String timestamp = question.get("timestamp").asText();
        System.out.println(titleSlug.equals(dailyChallengeTitleSlug));
        if (titleSlug.equals(dailyChallengeTitleSlug) && isSubmissionDateValid(timestamp)) {
          return NfycLcUserDailyStatusChallenge.SUBMITTED;
        }
      }
    } catch (RuntimeException e) {
      return NfycLcUserDailyStatusChallenge.ERROR;
    }
    return NfycLcUserDailyStatusChallenge.NOT_SUBMITTED;
  }

  private boolean isSubmissionDateValid(String timestampStr) {
    long timestamp = Long.parseLong(timestampStr);
    Date date = (Date.from(Instant.ofEpochSecond(timestamp)));
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
        throw new RuntimeException("Cant");
      }
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      try {
        dailyChallengeQuesDate = sdf.parse(date);
      } catch (ParseException pe) {
        throw new RuntimeException("Cannot parse");
      }
    }
  }
}
