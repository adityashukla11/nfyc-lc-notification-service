package com.nfyc.lcnotificationservice.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class GraphQLClientConfig {
    @Bean
    public HttpGraphQlClient nfycGraphQLClient() {
        WebClient client = WebClient.builder().baseUrl("https://leetcode.com/graphql").build();
        return HttpGraphQlClient.builder(client).build();
    }
}
