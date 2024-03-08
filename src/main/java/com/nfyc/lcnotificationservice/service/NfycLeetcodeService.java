package com.nfyc.lcnotificationservice.service;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nfyc.lcnotificationservice.domain.NfycLcResponse;
import com.nfyc.lcnotificationservice.domain.NfycLcUser;
import com.nfyc.lcnotificationservice.utils.NfycGraphQLQueries;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class NfycLeetcodeService {

  private final HttpGraphQlClient nfycGraphQLClient;
  private final ObjectMapper objectMapper;
  private final TableClient tableClient;
  private final NfycAzureTableQueryUtil nfycAzureTableQueryUtil;
  private final Validator validator;

  public NfycLcResponse getDailyChallenge() {
    return this.nfycGraphQLClient.document(NfycGraphQLQueries.GET_DAILY_CONTEST_INFO)
        .retrieve("activeDailyCodingChallengeQuestion").toEntity(JsonNode.class)
        .map(NfycLcResponse::new).block();
  }
  public NfycLcResponse saveLcUser(NfycLcUser nfycLcUser) {
    try {
      if (this.doesUserAlreadyExists(nfycLcUser)) {
        log.info("Cannot create user as user already exists");
        throw new RuntimeException("User Cannot be created");
      }
      Set<ConstraintViolation<NfycLcUser>> violations = validator.validate(nfycLcUser);
      if (!violations.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        for (ConstraintViolation<NfycLcUser> constraintViolation : violations) {
          sb.append(constraintViolation.getMessage());
        }
        throw new ConstraintViolationException("Error occurred: " + sb.toString(), violations);
      }
      tableClient.upsertEntity(convertNfycLcUserToTableEntity(nfycLcUser));
      ObjectNode response = objectMapper.createObjectNode();
      response.put("message", "User was successfully created");
      return new NfycLcResponse(response);
    } catch (ConstraintViolationException e) {
//      throw new RuntimeException(e.getMessage());
      throw e;
    }
  }

  public List<NfycLcUser> getAllNfycUser() {
    try {
      String partitionKey = "nfyclcuser";
      ListEntitiesOptions options = new ListEntitiesOptions()
          .setFilter(this.nfycAzureTableQueryUtil.getAllEntitiesByPartitionKey(partitionKey));
      return tableClient.listEntities(options, null, null)
          .stream().map(this::convertTableEntityToNfycLcUser).toList();
    } catch (ConstraintViolationException e) {
      throw new RuntimeException("I CANT DO IT");
    }
  }

  private NfycLcUser convertTableEntityToNfycLcUser(TableEntity tableEntity) {
    return NfycLcUser.builder()
        .userId(UUID.fromString(tableEntity.getProperty("userId").toString()))
        .email(tableEntity.getRowKey())
        .fullName(tableEntity.getProperty("fullName").toString())
        .lcUsername(tableEntity.getProperty("lcUsername").toString()).build();
  }

  private TableEntity convertNfycLcUserToTableEntity(NfycLcUser nfycLcUser) {
    String partitionKey = "nfyclcuser";
    String userId = UUID.randomUUID().toString();
    Map<String, Object> nfycLcUserProperty = new HashMap<>();
    nfycLcUserProperty.put("userId", userId);
    nfycLcUserProperty.put("email", nfycLcUser.getEmail());
    nfycLcUserProperty.put("fullName", nfycLcUser.getFullName());
    nfycLcUserProperty.put("lcUsername", nfycLcUser.getLcUsername());
    return new TableEntity(partitionKey, nfycLcUser.getEmail()).setProperties(nfycLcUserProperty);
  }

  private boolean doesUserAlreadyExists(NfycLcUser nfycLcUser) {
    String partitionKey = "nfyclcuser";
    try {
      TableEntity tableEntity = tableClient.getEntity(partitionKey, nfycLcUser.getEmail());
    } catch (TableServiceException tableServiceException) {
      return false;
    }
    return true;
  }
}
