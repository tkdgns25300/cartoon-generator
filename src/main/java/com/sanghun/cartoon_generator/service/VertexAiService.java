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
                .flatMap(response -> {
                    if (response != null && response.getPredictions() != null && !response.getPredictions().isEmpty()) {
                        String base64Image = response.getPredictions().get(0).getBytesBase64Encoded();
                        if (base64Image != null) {
                            return Mono.just(base64Image);
                        }
                    }
                    return Mono.empty();
                });
    }

    public Mono<String> getCharacterDescriptions(String story) throws IOException {
        String promptText = "You are a character designer. Read the following story and create a detailed visual description for each main character. " +
                "Describe their appearance, clothing, and key features in a consistent manner that can be used by an image generation AI. " +
                "The output should be a simple list of character descriptions. " +
                "Example: 'CHARACTER 1 (Brave Knight): A young man with short brown hair, wearing shining silver armor with a red cape. He has a determined expression.'\n" +
                "--- STORY ---\n" + story;

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

    public Mono<String> getPromptsFromStory(String story, String characterDescriptions, boolean includeDialogue) throws IOException {
        String dialogueInstruction = includeDialogue ?
                "For each of the 10 scenes, you MUST invent and include a short line of English dialogue in a speech bubble." :
                "You MUST NOT include any dialogue or text in the images.";

        String characterInstruction = "You MUST ensure all characters are visually consistent across all panels by using the following detailed descriptions. When a character appears in a scene, you MUST incorporate their full description from the list below directly into the prompt for that scene. Example: 'A brave knight (a young man with short brown hair, wearing shining silver armor and a red cape) enters a dark cave.'\n" +
                "--- CHARACTER DESCRIPTIONS ---\n" +
                characterDescriptions + "\n" +
                "----------------------------\n\n";

        String promptText = "You are a webtoon prompt engineer. Your task is to create 10 sequential image generation prompts based on the provided story and character descriptions.\n" +
                characterInstruction +
                "The output must be a numbered list of 10 prompts. Each prompt must be a single, continuous sentence. Do not add any introductory text.\n" +
                dialogueInstruction +
                "\n--- STORY ---\n" + story;

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