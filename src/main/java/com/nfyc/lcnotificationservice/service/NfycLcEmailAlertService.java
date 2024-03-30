package com.nfyc.lcnotificationservice.service;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import com.nfyc.lcnotificationservice.domain.NfycLcUser;
import com.nfyc.lcnotificationservice.domain.NfycLcUserDailyStatusChallenge;
import com.nfyc.lcnotificationservice.utils.NfycEmailBodyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class NfycLcEmailAlertService {

  private final TableClient tableClient;
  private final NfycLeetcodeService nfycLeetcodeService;
  private final NfycAzureTableQueryUtil nfycAzureTableQueryUtil;
  private final EmailClient emailClient;

  public String triggerLcEmailAlertsForUsers() {
      String partitionKey = "nfyclcuser";
      ListEntitiesOptions options = new ListEntitiesOptions()
          .setFilter(this.nfycAzureTableQueryUtil.getAllEntitiesByPartitionKey(partitionKey));
      List<NfycLcUser> user = tableClient.listEntities(options, null, null)
          .stream().map(this::convertTableEntityToNfycLcUser).toList();
      nfycLeetcodeService.setDailyChallengeInfo();
      ConcurrentMap<NfycLcUserDailyStatusChallenge, List<NfycLcUser>> userMap =
          user.parallelStream()
          .collect(Collectors.groupingByConcurrent(this.nfycLeetcodeService::hasUserSuccessfullySubmittedTheDailyChallenge));

      if (userMap.get(NfycLcUserDailyStatusChallenge.SUBMITTED) != null) {
        sendEmail(NfycLcUserDailyStatusChallenge.SUBMITTED, userMap.get(NfycLcUserDailyStatusChallenge.SUBMITTED));
        log.info("Triggering email for users who have successfully submitted the daily challenge");
      }

      if (userMap.get(NfycLcUserDailyStatusChallenge.NOT_SUBMITTED) != null) {
        log.info("Triggering email for users who have NOT submitted the daily challenge");
        sendEmail(NfycLcUserDailyStatusChallenge.NOT_SUBMITTED, userMap.get(NfycLcUserDailyStatusChallenge.NOT_SUBMITTED));
      }

      return "Executed the email alerts for today " + LocalDate.now();
    }

  private NfycLcUser convertTableEntityToNfycLcUser(TableEntity tableEntity) {
    return NfycLcUser.builder()
        .userId(UUID.fromString(tableEntity.getProperty("userId").toString()))
        .email(tableEntity.getRowKey())
        .fullName(tableEntity.getProperty("fullName").toString())
        .lcUsername(tableEntity.getProperty("lcUsername").toString()).build();
  }

  private void sendEmail(NfycLcUserDailyStatusChallenge nfycLcUserDailyStatusChallenge, List<NfycLcUser> users) {
    try {
      EmailMessage emailMessage = NfycEmailBodyFactory.getEmailMessage(nfycLcUserDailyStatusChallenge, users);
      SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(emailMessage, null);
      PollResponse<EmailSendResult> result = poller.waitForCompletion();
      log.info("Email Triggered Status: " + result.getStatus());
    } catch (Exception e) {
      log.error("Error occurred while sending the email : " + e.getMessage());
      throw e;
    }
  }
}
