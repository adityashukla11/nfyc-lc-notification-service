package com.nfyc.lcnotificationservice.utils;

public class NfycLcEcxeption extends RuntimeException {
  public NfycLcEcxeption(NfycLcError error, String... params) {
    super(String.format(error.getErrorMessage(), params));
  }
}