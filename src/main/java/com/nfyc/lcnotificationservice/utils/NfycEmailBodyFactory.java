package com.nfyc.lcnotificationservice.utils;

import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import com.nfyc.lcnotificationservice.domain.NfycLcUser;
import com.nfyc.lcnotificationservice.domain.NfycLcUserDailyStatusChallenge;

import java.util.List;

public class NfycEmailBodyFactory {
  private static final String notSubmittedEmail = """
      <!DOCTYPE html>
      <html lang="en">
      <head>
          <meta charset="UTF-8">
          <meta http-equiv="X-UA-Compatible" content="IE=edge">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Reminder to complete the Leetcode Daily Challenge</title>
      </head>
      <body>
          <p>
              Hey there,
          </p>
          <p>
             Just a friendly reminder that your submission for today's LeetCode daily challenge is still pending.
             <br>
             Completing it as soon as possible will help you maintain your streak!
          </p>
          <p>
             Keep up the good work and keep grinding!
          </p>
          <p>
              Thank you<br>
          </p>
      </body>
      </html>
      """;

  private static final String submittedEmail = """
      <!DOCTYPE html>
      <html lang="en">
      <head>
          <meta charset="UTF-8">
          <meta http-equiv="X-UA-Compatible" content="IE=edge">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Reminder to complete the Leetcode Daily Challenge</title>
      </head>
      <body>
          <p>
              Hey there,
          </p>
          <p>
            Congratulations on successfully submitting today's LeetCode daily challenge!
            <br>
          </p>
          <p>
             Keep up the good work and keep grinding!
          </p>
          <p>
              Thank you<br>
          </p>
      </body>
      </html>
      """;

  public static EmailMessage getEmailMessage(NfycLcUserDailyStatusChallenge nfycLcUserDailyStatusChallenge, List<NfycLcUser> recipientEmailAddresses) {
    List<EmailAddress> emailAddressList = recipientEmailAddresses.stream().map(nfycLcUser -> new EmailAddress(nfycLcUser.getEmail())).toList();
    EmailMessage emailMessage = new EmailMessage()
        .setSenderAddress(NfycLcConstants.SENDER_EMAIL_ADDRESS)
        .setBccRecipients(emailAddressList);
    if (nfycLcUserDailyStatusChallenge.equals(NfycLcUserDailyStatusChallenge.SUBMITTED)) {
      emailMessage.setSubject("Congratulations on Completing Today's LeetCode Challenge!");
      emailMessage.setBodyHtml(submittedEmail);
    } else if (nfycLcUserDailyStatusChallenge.equals(NfycLcUserDailyStatusChallenge.NOT_SUBMITTED)) {
      emailMessage.setSubject("Reminder to complete the Leetcode Daily Challenge");
      emailMessage.setBodyHtml(notSubmittedEmail);
    } else {
      throw new RuntimeException("The email type is not supported");
    }
    return emailMessage;
  }

}
