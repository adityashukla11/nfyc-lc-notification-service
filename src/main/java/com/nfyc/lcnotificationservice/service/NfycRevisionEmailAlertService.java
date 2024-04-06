package com.nfyc.lcnotificationservice.service;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableTransactionAction;
import com.azure.data.tables.models.TableTransactionActionType;
import com.nfyc.lcnotificationservice.domain.LcAttemptedQuestion;
import com.nfyc.lcnotificationservice.domain.NfycLcUser;
import com.nfyc.lcnotificationservice.utils.NfycEmailBodyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class NfycRevisionEmailAlertService {

  private final TableClient tableClient;
  private final NfycAzureTableQueryUtil nfycAzureTableQueryUtil;
  private final EmailClient emailClient;
  private final NfycRevisionAlgo nfycRevisionAlgo;

  public String triggerRevisionEmailAlert() {
    String partitionKey = "nfyclcuser";
    ListEntitiesOptions options = new ListEntitiesOptions()
        .setFilter(this.nfycAzureTableQueryUtil.getAllEntitiesByPartitionKey(partitionKey));
    List<NfycLcUser> user = tableClient.listEntities(options, null, null)
        .stream().map(this::convertTableEntityToNfycLcUser).toList();
    user.parallelStream()
        .flatMap(currentUser -> {
          String questionsPartitionKey = "ac_questions";
          options.setFilter(this.nfycAzureTableQueryUtil.getAllEntitiesByPartitionKeyStartingWithRowKey(questionsPartitionKey, currentUser.getEmail()));
          List<LcAttemptedQuestion> attemptedQuestions = tableClient.listEntities(options, null, null)
              .stream().map(this::convertTableEntityToLcAttemptedQuestion)
              .filter(question -> this.nfycRevisionAlgo.isNextRevisionDayLessThanToday(question.getNextRevisionDate()))
              .sorted(this.nfycRevisionAlgo.getNfycRevisionComparator()).toList();
          List<LcAttemptedQuestion> result = extractQuestionsForRevision(attemptedQuestions);
          if (!result.isEmpty()) {
            EmailMessage emailMessage = NfycEmailBodyFactory.getRevisionEmailBody(result, currentUser.getEmail());
            sendEmail(emailMessage);
          }
          return result.stream();
        }).map(lcAttemptedQuestion -> {
          Map<String, Object> nfycLcUserQuestion = new HashMap<>();
          nfycLcUserQuestion.put("questionTitleSlug", lcAttemptedQuestion.getQuestionTitleSlug());
          nfycLcUserQuestion.put("questionTitle", lcAttemptedQuestion.getQuestionTitle());
          nfycLcUserQuestion.put("lastSolvedDate", lcAttemptedQuestion.getLastSolvedDate());
          nfycLcUserQuestion.put("lastRevisedDate", new Date());
          nfycLcUserQuestion.put("priority",  lcAttemptedQuestion.getPriority());
          nfycLcUserQuestion.put("revisionCount", lcAttemptedQuestion.getRevisionCount() + 1);
          nfycLcUserQuestion.put("nextRevisionDate", this.nfycRevisionAlgo.getNextRevisionDate(new Date(), lcAttemptedQuestion.getPriority(),
              lcAttemptedQuestion.getRevisionCount() + 1));
          return new TableTransactionAction(
              TableTransactionActionType.UPDATE_MERGE,
              new TableEntity("ac_questions", lcAttemptedQuestion.getRowKey())
                  .setProperties(nfycLcUserQuestion));
        }).collect(Collectors.collectingAndThen(Collectors.toList(), tableClient::submitTransaction));
    return "Completed";
  }

  private boolean sendEmail(EmailMessage emailMessage) {
    try {
      SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(emailMessage, null);
      PollResponse<EmailSendResult> result = poller.waitForCompletion();
      return true;
    } catch (Exception e) {
//      log.error("Error occurred while sending the email : " + e.getMessage());
      return false;
    }
  }
  private NfycLcUser convertTableEntityToNfycLcUser(TableEntity tableEntity) {
    return NfycLcUser.builder()
        .userId(UUID.fromString(tableEntity.getProperty("userId").toString()))
        .email(tableEntity.getRowKey())
        .lcUsername(tableEntity.getProperty("lcUsername").toString()).build();
  }

  private List<LcAttemptedQuestion> extractQuestionsForRevision(List<LcAttemptedQuestion> attemptedQuestions) {
    if (attemptedQuestions.size() < 4) {
      return attemptedQuestions;
    }
    Set<LcAttemptedQuestion> taken = new HashSet<>();
    int lastTakenIndex = 0;
    int size = attemptedQuestions.size();
    for (int i = 0; i < size; i++) {
      if (i == 0 || attemptedQuestions.get(i).getPriority() != attemptedQuestions.get(i - 1).getPriority()) {
        taken.add(attemptedQuestions.get(i));
        lastTakenIndex = i;
      }
      if (taken.size() == 3) {
        return new ArrayList<>(taken);
      }
    }
    for (int j = lastTakenIndex + 1; j < 2 * attemptedQuestions.size(); j++) {
      taken.add(attemptedQuestions.get(j % size));
      if (taken.size() == 3) {
        return new ArrayList<>(taken);
      }
    }
    return new ArrayList<>(taken);
  }

  public LcAttemptedQuestion convertTableEntityToLcAttemptedQuestion(TableEntity tableEntity) {
    return LcAttemptedQuestion.builder()
        .rowKey(tableEntity.getRowKey())
        .lastRevisedDate(getDateFromOffsetDateTime(tableEntity.getProperty("lastRevisedDate")))
        .nextRevisionDate(getDateFromOffsetDateTime(tableEntity.getProperty("nextRevisionDate")))
        .lastSolvedDate(getDateFromOffsetDateTime(tableEntity.getProperty("lastSolvedDate")))
        .priority((int) (tableEntity.getProperty("priority")))
        .revisionCount((int) tableEntity.getProperty("revisionCount"))
        .questionTitle(tableEntity.getProperty("questionTitle").toString())
        .questionTitleSlug(tableEntity.getProperty("questionTitleSlug").toString())
        .build();
  }

  private Date getDateFromOffsetDateTime(Object object) {
    OffsetDateTime offsetDateTime = (OffsetDateTime) object;
    return Date.from(offsetDateTime.toInstant());
  }
}
