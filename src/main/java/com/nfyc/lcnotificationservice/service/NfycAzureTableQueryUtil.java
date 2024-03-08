package com.nfyc.lcnotificationservice.service;

import org.springframework.stereotype.Service;

@Service
public class NfycAzureTableQueryUtil {

  public String getAllEntitiesByPartitionKey(String partitionKey) {
      String query = "PartitionKey eq '%s'";
      return query.formatted(partitionKey);
  }
}

