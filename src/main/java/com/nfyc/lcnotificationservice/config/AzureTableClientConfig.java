package com.nfyc.lcnotificationservice.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.identity.DefaultAzureCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@RequiredArgsConstructor
@Configuration
public class AzureTableClientConfig {
  private static final String TABLE_NAME = "nfyclcnotifications";
  private final DefaultAzureCredential defaultAzureCredential;

  @Bean
  @Scope(value = "singleton")
  public TableClient nfycAzureTableClient() {
    return new TableClientBuilder()
        .endpoint("https://lcnotifierstorage.table.core.windows.net")
        .credential(defaultAzureCredential)
        .tableName(TABLE_NAME)
        .buildClient();
  }

}
