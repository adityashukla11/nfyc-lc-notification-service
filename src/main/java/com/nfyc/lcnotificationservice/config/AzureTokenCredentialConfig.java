package com.nfyc.lcnotificationservice.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.nfyc.lcnotificationservice.utils.NfycLcConstants.NFYC_MANAGED_IDENTITY_CLIENT_ID;

@Slf4j
@Configuration
public class AzureTokenCredentialConfig {
  @Bean
  public DefaultAzureCredential getTokenCredential() {
    return new DefaultAzureCredentialBuilder()
        .managedIdentityClientId(NFYC_MANAGED_IDENTITY_CLIENT_ID).build();
  }
}
