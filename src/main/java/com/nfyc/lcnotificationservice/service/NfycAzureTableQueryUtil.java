package com.nfyc.lcnotificationservice.service;

import org.springframework.stereotype.Service;

@Service
public class NfycAzureTableQueryUtil {

  public String getAllEntitiesByPartitionKey(String partitionKey) {
      String query = "PartitionKey eq '%s'";
      return query.formatted(partitionKey);
  }

  public String getAllEntitiesByPartitionKeyStartingWithRowKey(String partitionKey, String rowKey) {
    String modifiedRowKey = incrementLastCharacterOfRowKey(rowKey);
    String query = "PartitionKey eq '%s' and (RowKey ge '%s' and RowKey le '%s')";
    return query.formatted(partitionKey, rowKey, modifiedRowKey);
  }

  public String incrementLastCharacterOfRowKey(String rowKey) {
    int lastIndex = rowKey.length() - 1;
    char ch = (char) (rowKey.charAt(lastIndex) + 1);
    return rowKey.substring(0, lastIndex) + ch;
  }
}

