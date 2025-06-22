package com.sanghun.cartoon_generator.service;

import com.sanghun.cartoon_generator.dto.*;
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
    private final String geminiModelId;
    private final String imagenModelId;
    private final GoogleCredentials credentials;

    public VertexAiService(RestTemplate restTemplate,
                           @Value("${google.cloud.project-id}") String projectId,
                           @Value("${google.cloud.region}") String region,
                           @Value("${google.cloud.gemini-model-id}") String geminiModelId,
                           @Value("${google.cloud.imagen-model-id}") String imagenModelId) throws IOException {
        this.restTemplate = restTemplate;
        this.projectId = projectId;
        this.region = region;
        this.geminiModelId = geminiModelId;
        this.imagenModelId = imagenModelId;
        this.credentials = GoogleCredentials.getApplicationDefault()
                .createScoped("https://www.googleapis.com/auth/cloud-platform");
    }

    public String getCharacterDescriptions(String story) throws IOException {
        log.info("Generating character descriptions for story...");
        String prompt = "Analyze the main characters from the following story and create a detailed character sheet. " +
                "For each character, describe their name, personality, and key visual features like clothing, species, and distinct marks. " +
                "This sheet will be used to maintain character consistency across multiple images.\n\n" +
                "Story: \"" + story + "\"";

        GeminiRequest geminiRequest = GeminiRequest.fromPrompt(prompt);
        String url = String.format(GEMINI_API_ENDPOINT_TEMPLATE, region, projectId, region, geminiModelId);

        ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(url, new HttpEntity<>(geminiRequest, createHeaders()), GeminiResponse.class);
        log.info("Successfully generated character descriptions.");
        return getTextFromGeminiResponse(response.getBody());
    }

    public List<String> getPanelPrompts(String story, String characterDescriptions, boolean includeDialogue) throws IOException {
        log.info("Generating 10 panel prompts...");
        String prompt = "Based on the provided story, divide it into exactly 10 sequential scenes and create a concise image generation prompt for each scene. " +
                "These prompts will be used with a character reference image, so DO NOT include character descriptions in the prompts. Focus only on the action, setting, and mood of each scene. " +
                "To avoid safety policy issues, do not use words that could be misinterpreted, such as 'dome', 'shaft', or words related to violence or anatomy. " +
                "CRITICAL: Generate exactly 10 prompts separated by '---'. Do not include any introductory text, titles, or numbering. Just the prompts.";

        if (includeDialogue) {
            prompt += "\nFor each prompt, add a simple, single-line dialogue in English inside quotation marks, suitable for a comic panel.";
        }

        GeminiRequest geminiRequest = GeminiRequest.fromPrompt(prompt);
        String url = String.format(GEMINI_API_ENDPOINT_TEMPLATE, region, projectId, region, geminiModelId);

        ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(url, new HttpEntity<>(geminiRequest, createHeaders()), GeminiResponse.class);
        String fullResponse = getTextFromGeminiResponse(response.getBody());
        log.info("Successfully generated 10 panel prompts.");
        return Arrays.stream(fullResponse.split("---"))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.toLowerCase().contains("scenes with image generation prompts"))
                .collect(Collectors.toList());
    }

    public String generateCharacterReferenceImage(String characterDescriptions) throws IOException {
        log.info("Generating character reference image...");
        String prompt = "A character sheet of all main characters described below, standing side-by-side, full body shot, plain background, cartoon style. " +
                "Characters: " + characterDescriptions;

        ImagenRequest imagenRequest = ImagenRequest.fromInstance(Instance.fromPrompt(prompt));
        String url = String.format(IMAGEN_API_ENDPOINT_TEMPLATE, region, projectId, region, imagenModelId);
        ResponseEntity<ImagenResponse> response = restTemplate.postForEntity(url, new HttpEntity<>(imagenRequest, createHeaders()), ImagenResponse.class);

        if (response.getBody() != null && response.getBody().getPredictions() != null && !response.getBody().getPredictions().isEmpty()) {
            String base64Image = response.getBody().getPredictions().get(0).getBytesBase64Encoded();
            log.info("Successfully generated character reference image.");
            return base64Image;
        }
        throw new IOException("Failed to generate character reference image from Vertex AI");
    }

    public String generateImageFromPromptAndReference(String prompt, String referenceImageBase64) throws IOException {
        Instance instance = Instance.fromPromptAndImage(prompt, referenceImageBase64);
        ImagenRequest imagenRequest = ImagenRequest.fromInstance(instance);

        // NOTE: Use the model that supports image editing/reference. This might be a different model ID.
        String url = String.format(IMAGEN_API_ENDPOINT_TEMPLATE, region, projectId, region, imagenModelId);

        ResponseEntity<ImagenResponse> response = restTemplate.postForEntity(url, new HttpEntity<>(imagenRequest, createHeaders()), ImagenResponse.class);

        if (response.getBody() != null && response.getBody().getPredictions() != null && !response.getBody().getPredictions().isEmpty()) {
            return response.getBody().getPredictions().get(0).getBytesBase64Encoded();
        }
        throw new IOException("Failed to generate image from prompt and reference from Vertex AI");
    }

    public String generateSingleImage(String prompt) throws IOException {
        log.info("Generating single image for prompt: {}", prompt);
        Instance instance = Instance.fromPrompt(prompt);
        ImagenRequest imagenRequest = ImagenRequest.fromInstance(instance);
        String url = String.format(IMAGEN_API_ENDPOINT_TEMPLATE, region, projectId, region, imagenModelId);
        ResponseEntity<ImagenResponse> response = restTemplate.postForEntity(url, new HttpEntity<>(imagenRequest, createHeaders()), ImagenResponse.class);

        if (response.getBody() != null && response.getBody().getPredictions() != null && !response.getBody().getPredictions().isEmpty()) {
            String base64Image = response.getBody().getPredictions().get(0).getBytesBase64Encoded();
            log.info("Successfully generated single image.");
            return base64Image;
        }
        throw new IOException("Failed to generate single image from Vertex AI");
    }

    private String getTextFromGeminiResponse(GeminiResponse response) {
        if (response != null && response.getFirstCandidateText() != null) {
            return response.getFirstCandidateText();
        }
        throw new IllegalStateException("Invalid GeminiResponse format or empty content.");
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