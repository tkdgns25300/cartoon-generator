package com.sanghun.cartoon_generator.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.sanghun.cartoon_generator.dto.ImagenRequest;
import com.sanghun.cartoon_generator.dto.ImagenResponse;
import com.sanghun.cartoon_generator.dto.Instance;
import com.sanghun.cartoon_generator.dto.Parameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
public class ImageGenerationService {

    private final WebClient webClient;
    private final String projectId;
    private final String apiUrl;

    public ImageGenerationService(WebClient.Builder webClientBuilder,
            @Value("${google.cloud.project-id}") String projectId) {

        final int bufferSize = 16 * 1024 * 1024; // 16MB

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(bufferSize))
                .build();

        this.webClient = webClientBuilder
                .exchangeStrategies(exchangeStrategies)
                .build();
        this.projectId = projectId;
        this.apiUrl = String.format(
                "https://us-central1-aiplatform.googleapis.com/v1/projects/%s/locations/us-central1/publishers/google/models/imagen-4.0-generate-preview-06-06:predict",
                projectId);
    }

    public Mono<String> generateImage(String prompt) throws IOException {
        ImagenRequest request = new ImagenRequest(
                Collections.singletonList(new Instance(prompt)),
                new Parameters(1, true));

        return webClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ImagenResponse.class)
                .map(response -> {
                    if (response != null && response.getPredictions() != null && !response.getPredictions().isEmpty()) {
                        return response.getPredictions().get(0).getBytesBase64Encoded();
                    }
                    return null;
                });
    }

    private String getAccessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }
}