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

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    public VertexAiService(RestTemplate restTemplate,
            @Value("${google.cloud.project-id}") String projectId,
            @Value("${google.cloud.region}") String region,
            @Value("${google.cloud.imagen-model-id}") String imagenModelId,
            @Value("${google.cloud.gemini-model-id}") String geminiModelId) throws IOException {
        this.restTemplate = restTemplate;
        this.projectId = projectId.trim();
        this.region = region.trim();
        this.imagenModelId = imagenModelId.trim();
        this.geminiModelId = geminiModelId.trim();
        this.credentials = GoogleCredentials.getApplicationDefault()
                .createScoped("https://www.googleapis.com/auth/cloud-platform");
    }

    public String generateSingleImage(String prompt) throws IOException {
        log.info("Generating single image for prompt: {}", prompt);
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Instance instance = Instance.fromPrompt(prompt);
                ImagenRequest imagenRequest = ImagenRequest.fromInstance(instance);
                String url = String.format(IMAGEN_API_ENDPOINT_TEMPLATE, region, projectId, region, imagenModelId);
                ResponseEntity<ImagenResponse> response = restTemplate.postForEntity(url,
                        new HttpEntity<>(imagenRequest, createHeaders()), ImagenResponse.class);

                if (response.getBody() != null && response.getBody().getPredictions() != null
                        && !response.getBody().getPredictions().isEmpty()) {
                    String base64Image = response.getBody().getPredictions().get(0).getBytesBase64Encoded();
                    log.info("Successfully generated single image on attempt {}/{}", attempt, MAX_RETRIES);
                    return base64Image;
                } else {
                    throw new IOException("Vertex AI returned a response with no image data.");
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed to generate image for prompt: {}. Error: {}", attempt, MAX_RETRIES, prompt, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Image generation retry was interrupted", ie);
                    }
                }
            }
        }
        throw new IOException("Failed to generate single image from Vertex AI after " + MAX_RETRIES + " attempts for prompt: " + prompt, lastException);
    }

    public List<String> generateStoryPrompts(String storyIdea) throws IOException {
        log.info("Generating 10 story prompts from idea: {}", storyIdea);
        String prompt = "You are an expert prompt engineer for a text-to-image model, specializing in creating coherent, multi-panel stories with high character consistency.\n"
                +
                "Your task is to create a series of 10 detailed, consistent, and vivid prompts for a 10-panel cartoon based on the following story idea.\n\n"
                +
                "STORY IDEA: \"" + storyIdea + "\"\n\n" +
                "## CRITICAL INSTRUCTIONS:\n" +
                "1.  **Analyze and Create a Character Sheet (Internal):** First, identify the main characters. For each character, create a detailed 'Character Sheet' for your internal use. This sheet must define consistent attributes that you will use in EVERY prompt where the character appears.\n\n"
                +
                "2.  **Enforce Strict Consistency Using the Character Sheet:**\n" +
                "    *   **Unique Name/Role:** Use a unique, memorable name like 'Bruno the Bear' or 'Luna the Rabbit'.\n"
                +
                "    *   **Species/Body Ratio:** Fix the size ratio, e.g., 'Bruno stands twice Luna's height'.\n" +
                "    *   **Colors (Fur, Eyes, etc.):** Be specific and unchanging, e.g., 'chestnut-brown fur, hazel eyes'.\n"
                +
                "    *   **Signature Outfit:** A defining, unchanging piece of clothing, e.g., 'a mint-green knit scarf'.\n"
                +
                "    *   **Props/Accessories:** Unique items they always carry, e.g., 'a vintage leather camera'.\n" +
                "    *   **Distinctive Markings/Hair:** A fixed physical feature, e.g., 'a white patch over the left ear'.\n"
                +
                "    *   **Personality Keywords:** Use recurring keywords for expressions, e.g., 'a gentle, curious smile'.\n\n"
                +
                "3.  **Enforce Scene and Style Consistency:**\n" +
                "    *   **Overall Style:** The style must be consistent throughout all 10 panels. Specify a single, clear style, e.g., 'charming children's book illustration', 'digital art, whimsical and vibrant', or 'Studio Ghibli anime style'. This style component is mandatory for every prompt.\n"
                +
                "    *   **Fixed Color Palette:** Maintain a consistent color palette, e.g., 'pastel greens, yellows, peach highlights'.\n"
                +
                "    *   **Viewpoint/Camera Angle:** Keep a relatively stable camera perspective, e.g., 'eye-level three-quarter view'.\n\n"
                +
                "4.  **Negative Prompts:** Use negative prompts to prevent unwanted variations, e.g., '--no outfit changes, --no different fur colors'.\n\n"
                +
                "5.  **Output Format and Structure:** Each of the 10 prompts you generate must be explicitly structured to contain these three core elements: **Style, Subject, and Context/Background.** For example: 'Style: Studio Ghibli anime style. Subject: Bruno the Bear giving Luna the Rabbit a flower. Context: in a sun-dappled forest clearing.'\n"
                +
                "    Your final output MUST be ONLY the 10 structured prompts, separated by '---'. Do NOT include the character sheet, titles, reasoning, or any other extra text in your response. Just the prompts, separated by '---'.\n\n"
                +
                "Now, generate the 10 prompts.";

        GeminiRequest geminiRequest = GeminiRequest.fromPrompt(prompt);
        String url = String.format(GEMINI_API_ENDPOINT_TEMPLATE, region, projectId, region, geminiModelId);

        ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(url,
                new HttpEntity<>(geminiRequest, createHeaders()), GeminiResponse.class);

        String fullResponse = getTextFromGeminiResponse(response.getBody());
        if (fullResponse != null) {
            log.info("Successfully generated 10 consistent story prompts.");
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