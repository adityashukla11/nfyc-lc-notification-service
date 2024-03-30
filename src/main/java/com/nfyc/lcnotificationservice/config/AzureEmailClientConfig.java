package com.nfyc.lcnotificationservice.config;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.ManagedIdentityCredential;
import com.nfyc.lcnotificationservice.utils.NfycLcConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@RequiredArgsConstructor
@Configuration
public class AzureEmailClientConfig {

  private final DefaultAzureCredential defaultAzureCredential;
  @Bean
  @Scope(value = "singleton")
  public EmailClient nfycAzureEmailClient() {
    return new EmailClientBuilder().endpoint(NfycLcConstants.AZURE_COMMUNICATION_SERVICE_ENDPOINT)
        .credential(defaultAzureCredential)
        .buildClient();
  }
}
