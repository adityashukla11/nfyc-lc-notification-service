package com.nfyc.lcnotificationservice.utils;

public enum NfycLcError {

  //Application

  //Validation
  ERROR_USER_EMAIL_ALREADY_EXIST ("The user email %s already exists. Failed to create user"),
  ERROR_LC_USERNAME_INVALID ("The leetcode username %s entered by user is invalid. Failed to create user"),
  ERROR_USER_CREATION_FAILED ("Error occurred while creating user: %s"),

  ERROR_LC_API_REQUEST_FAILED("Something went wrong. Please try again later"),
  ERROR_NO_API_PAYLOAD ("No API Payload was specified");
  private final String errorMessage;

  NfycLcError(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage() {
    return this.errorMessage;
  }
}
