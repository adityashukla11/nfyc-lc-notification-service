package com.nfyc.lcnotificationservice.utils;

public class NfycLcEcxeption extends RuntimeException {
  private NfycLcError errorCode;
  public NfycLcEcxeption(NfycLcError error, String... params) {
    super(String.format(error.getErrorMessage(), params));
    errorCode = error;
  }
  public NfycLcError getErrorCode() {
    return errorCode;
  }
}