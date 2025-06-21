package com.sanghun.cartoon_generator.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.sanghun.cartoon_generator.dto.*;
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
public class VertexAiService {

    private final WebClient webClient;
    private final String projectId;
    private final String imagenApiUrl;
    private final String geminiApiUrl;

    public VertexAiService(WebClient.Builder webClientBuilder,
                           @Value("${google.cloud.project-id}") String projectId,
                           @Value("${google.cloud.imagen-model-id}") String imagenModelId,
                           @Value("${google.cloud.gemini-model-id}") String geminiModelId) {

        final int bufferSize = 16 * 1024 * 1024; // 16MB

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(bufferSize))
                .build();

        this.webClient = webClientBuilder
                .exchangeStrategies(exchangeStrategies)
                .build();
        this.projectId = projectId;
        this.imagenApiUrl = String.format(
                "https://us-central1-aiplatform.googleapis.com/v1/projects/%s/locations/us-central1/publishers/google/models/%s:predict",
                projectId, imagenModelId);
        this.geminiApiUrl = String.format(
                "https://us-central1-aiplatform.googleapis.com/v1/projects/%s/locations/us-central1/publishers/google/models/%s:generateContent",
                projectId, geminiModelId);
    }

    public Mono<String> generateImage(String prompt) throws IOException {
        ImagenRequest request = new ImagenRequest(
                Collections.singletonList(new Instance(prompt)),
                new Parameters(1, true));

        return webClient.post()
                .uri(imagenApiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
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

    public Mono<String> getPromptsFromStory(String story) throws IOException {
        String promptText = "Divide the following story into 10 sequential scenes for a webtoon. " +
                "For each scene, provide a concise and descriptive prompt for an image generation AI. " +
                "The output should be a numbered list of prompts, with each prompt on a new line, like '1. prompt a', '2. prompt b'. " +
                "Do not include any other text or explanations. " +
                "Story: " + story;

        GeminiRequest.Part part = new GeminiRequest.Part(promptText);
        GeminiRequest.Content content = new GeminiRequest.Content("user", Collections.singletonList(part));
        GeminiRequest.GenerationConfig config = new GeminiRequest.GenerationConfig(1.0f, 32, 1, 8192, Collections.emptyList());
        GeminiRequest request = new GeminiRequest(content, config);

        return webClient.post()
                .uri(geminiApiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .map(GeminiResponse::getFirstCandidateText);
    }

    private String getAccessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }
} 