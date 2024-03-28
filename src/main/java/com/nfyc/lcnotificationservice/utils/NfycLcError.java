package com.nfyc.lcnotificationservice.utils;

public enum NfycLcError {

  ERROR_FILE_RETRIEVAL_FAILED("Error occurred while fetching file from %s container with error message %s"),
  ERROR_EMAIL_SENDING_FAILED("Error while sending email to client. The error message is:  %s");

  private final String errorMessage;

  NfycLcError(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage() {
    return this.errorMessage;
  }
}
