package com.sanghun.cartoon_generator.service;

import com.sanghun.cartoon_generator.dto.GeminiRequest;
import com.sanghun.cartoon_generator.dto.GeminiResponse;
import com.sanghun.cartoon_generator.dto.ImagenRequest;
import com.sanghun.cartoon_generator.dto.ImagenResponse;
import com.sanghun.cartoon_generator.dto.Instance;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VertexAiService {

    private static final String IMAGEN_API_ENDPOINT_TEMPLATE = "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predict";
    private static final String GEMINI_API_ENDPOINT_TEMPLATE = "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent";

    private final RestTemplate restTemplate;
    private final String projectId;
    private final String region;
    private final String imagenModelId;
    private final String geminiModelId;
    private final GoogleCredentials credentials;

    public VertexAiService(RestTemplate restTemplate,
            @Value("${google.cloud.project-id}") String projectId,
            @Value("${google.cloud.region}") String region,
            @Value("${google.cloud.imagen-model-id}") String imagenModelId,
            @Value("${google.cloud.gemini-model-id}") String geminiModelId) throws IOException {
        this.restTemplate = restTemplate;
        this.projectId = projectId;
        this.region = region;
        this.imagenModelId = imagenModelId;
        this.geminiModelId = geminiModelId;
        this.credentials = GoogleCredentials.getApplicationDefault()
                .createScoped("https://www.googleapis.com/auth/cloud-platform");
    }

    public String generateSingleImage(String prompt) throws IOException {
        log.info("Generating single image for prompt: {}", prompt);
        Instance instance = Instance.fromPrompt(prompt);
        ImagenRequest imagenRequest = ImagenRequest.fromInstance(instance);
        String url = String.format(IMAGEN_API_ENDPOINT_TEMPLATE, region, projectId, region, imagenModelId);
        ResponseEntity<ImagenResponse> response = restTemplate.postForEntity(url,
                new HttpEntity<>(imagenRequest, createHeaders()), ImagenResponse.class);

        if (response.getBody() != null && response.getBody().getPredictions() != null
                && !response.getBody().getPredictions().isEmpty()) {
            String base64Image = response.getBody().getPredictions().get(0).getBytesBase64Encoded();
            log.info("Successfully generated single image.");
            return base64Image;
        }
        throw new IOException("Failed to generate single image from Vertex AI");
    }

    public List<String> generateStoryPrompts(String storyIdea) throws IOException {
        log.info("Generating 10 story prompts from idea: {}", storyIdea);
        String prompt = "You are a creative storyteller. Based on the following theme or story idea, create a sequence of exactly 10 scenes that form a coherent short story. "
                +
                "For each scene, write a detailed and vivid image generation prompt. The prompts should be sequential and build upon each other. "
                +
                "CRITICAL: Separate each of the 10 prompts with '---'. Do not include any introductory text, titles, or numbering. Just the 10 prompts separated by '---'.\n\n"
                +
                "Story Idea: \"" + storyIdea + "\"";

        GeminiRequest geminiRequest = GeminiRequest.fromPrompt(prompt);
        String url = String.format(GEMINI_API_ENDPOINT_TEMPLATE, region, projectId, region, geminiModelId);

        ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(url,
                new HttpEntity<>(geminiRequest, createHeaders()), GeminiResponse.class);

        String fullResponse = getTextFromGeminiResponse(response.getBody());
        if (fullResponse != null) {
            log.info("Successfully generated 10 story prompts.");
            return Arrays.stream(fullResponse.split("---"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        log.warn("Failed to generate 10 story prompts from Vertex AI. Response was empty.");
        return Collections.emptyList();
    }

    private String getTextFromGeminiResponse(GeminiResponse response) {
        if (response != null && response.getFirstCandidateText() != null) {
            return response.getFirstCandidateText();
        }
        return null;
    }

    private HttpHeaders createHeaders() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken());
        return headers;
    }

    private String getAccessToken() throws IOException {
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }
}