package com.sanghun.cartoon_generator.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.sanghun.cartoon_generator.dto.JobStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        return GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
    }

    @Bean
    public ConcurrentHashMap<String, JobStatus> jobStatuses() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public WebClient webClient(GoogleCredentials credentials) {
        ExchangeFilterFunction authHeaderFilter = (clientRequest, next) -> {
            return Mono.fromCallable(() -> {
                        credentials.refreshIfExpired();
                        return credentials.getAccessToken().getTokenValue();
                    }).flatMap(token -> {
                        ClientRequest authorizedRequest = ClientRequest.from(clientRequest)
                                .headers(headers -> headers.setBearerAuth(token))
                                .build();
                        return next.exchange(authorizedRequest);
                    });
        };

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .exchangeStrategies(exchangeStrategies)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(authHeaderFilter)
                .build();
    }
} 