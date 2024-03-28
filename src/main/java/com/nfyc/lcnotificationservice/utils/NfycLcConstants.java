package com.nfyc.lcnotificationservice.utils;

public class NfycLcConstants {
  public static final String NFYC_MANAGED_IDENTITY_CLIENT_ID = System.getenv("FILE_MONITORING_MANAGED_IDENTITY_CLIENT_ID");
  public static final String AZURE_COMMUNICATION_SERVICE_ENDPOINT = System.getenv("AZURE_COMMUNICATION_SERVICE_ENDPOINT");
  public static final String SENDER_EMAIL_ADDRESS = System.getenv("SENDER_EMAIL_ADDRESS");


  public static final int RECORD_LIMIT = 20;
}
