package com.sanghun.cartoon_generator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanghun.cartoon_generator.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VertexAiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${google.cloud.project-id}")
    private String projectId;

    @Value("${google.cloud.region}")
    private String location;

    @Value("${google.cloud.gemini-model-id}")
    private String geminiModel;

    @Value("${google.cloud.imagen-model-id}")
    private String imagenModel;

    public Mono<List<String>> generateConsistentPrompts(String story, boolean includeDialog) {
        String promptText = buildConsistencyPrompt(story, includeDialog);

        GeminiRequest.GenerationConfig generationConfig = new GeminiRequest.GenerationConfig();
        generationConfig.setResponseMimeType("application/json");
        generationConfig.setMaxOutputTokens(16384);

        GeminiRequest request = new GeminiRequest(
                List.of(new GeminiRequest.Content(
                        "user",
                        List.of(new GeminiRequest.Part(promptText))
                )),
                generationConfig
        );

        return webClient.post()
                .uri(String.format("https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent", location, projectId, location, geminiModel))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .map(this::extractPromptsFromResponse)
                .doOnError(e -> log.error("Error calling Gemini API: {}", e.getMessage(), e));
    }

    private String buildConsistencyPrompt(String story, boolean includeDialog) {
        return "You are an expert prompt engineer for a text-to-image model." +
                "Your task is to create a series of 10 detailed, consistent, and vivid prompts for a 10-panel cartoon based on the following story.\n" +
                "STORY: \"" + story + "\"\n\n" +
                "## CRITICAL INSTRUCTIONS:\n" +
                "1.  **Analyze the Story**: First, identify the main characters and their key features from the story. Create a detailed 'Character Sheet' for internal use. This sheet must define consistent attributes for each character that you will use in EVERY prompt they appear in.\n" +
                "2.  **Enforce Consistency using a Character Sheet**: For each character, define and RE-USE the following attributes across all 10 prompts:\n" +
                "    *   **Unique Name/Role**: e.g., 'Bruno the Bear', 'Luna the Rabbit'.\n" +
                "    *   **Species/Body Ratio**: e.g., 'Bruno stands twice Luna's height'.\n" +
                "    *   **Fur/Eye/Nose Color**: Be specific, e.g., 'chestnut-brown fur, hazel eyes'.\n" +
                "    *   **Signature Outfit**: A defining, unchanging piece of clothing, e.g., 'a mint-green knit scarf', 'a yellow polka-dot sundress'.\n" +
                "    *   **Props/Accessories**: Unique items they carry, e.g., 'a vintage leather camera'.\n" +
                "    *   **Distinctive Patterns/Hair**: e.g., 'a white patch over the left ear'.\n" +
                "    *   **Expression/Personality Keywords**: e.g., 'a gentle, curious smile'.\n" +
                "3.  **Overall Scene Consistency**:\n" +
                "    *   **Fixed Color Palette**: Maintain a consistent color palette throughout. e.g., 'pastel greens, yellows, peach highlights'.\n" +
                "    *   **Viewpoint/Camera Angle**: Keep a relatively stable camera perspective, e.g., 'eye-level three-quarter view'.\n" +
                "    *   **Style**: The overall style must be consistent. Specify a style like 'charming children's book illustration', 'digital art, whimsical and vibrant', or 'Studio Ghibli anime style'.\n" +
                "4.  **Dialogue**: " + (includeDialog ? "If the panel requires dialogue, render it as clean, legible text inside a classic speech bubble. The font should be consistent." : "Do not include any dialogue or text in the images.") + "\n" +
                "5.  **Negative Prompts**: Use negative prompts to avoid unwanted variations. For example: '--no outfit changes, --no different fur colors, --no text, --no watermark'.\n" +
                "6.  **Output Format**: Your final output MUST be a JSON object with a single key \"prompts\" which contains an array of 10 strings. Each string is a final, detailed prompt for one panel. Do not include the character sheet in the final JSON output. Just use it to build the prompts.\n\n" +
                "## EXAMPLE OUTPUT FORMAT:\n" +
                "```json\n" +
                "{\n" +
                "  \"prompts\": [\n" +
                "    \"Prompt for panel 1...\",\n" +
                "    \"Prompt for panel 2...\",\n" +
                "    \"...and so on for 10 panels.\"\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                "Now, based on the story and all these rules, generate the 10 prompts.";
    }

    private List<String> extractPromptsFromResponse(GeminiResponse response) {
        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            log.warn("Received empty or invalid response from Gemini API.");
            return Collections.emptyList();
        }
        String jsonText = response.getFirstCandidateText()
                .trim()
                .replace("```json", "")
                .replace("```", "")
                .trim();

        try {
            PromptsResponse promptsResponse = objectMapper.readValue(jsonText, PromptsResponse.class);
            if (promptsResponse != null && promptsResponse.getPrompts() != null) {
                return promptsResponse.getPrompts();
            }
            log.warn("Parsed prompts response is null or does not contain prompts: {}", jsonText);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to parse JSON prompts from Gemini response: {}", jsonText, e);
            return Collections.emptyList();
        }
    }

    public Mono<String> generateImage(String prompt) {
        ImagenRequest request = new ImagenRequest(
                new Instance(prompt),
                new Parameters(1)
        );

        return webClient.post()
                .uri(String.format("https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predict", location, projectId, location, imagenModel))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ImagenResponse.class)
                .flatMap(response -> {
                    if (response != null && response.getPredictions() != null && !response.getPredictions().isEmpty()) {
                        String imageBytes = response.getPredictions().get(0).getBytesBase64Encoded();
                        if (imageBytes != null) {
                            return Mono.just(imageBytes);
                        }
                    }
                    log.warn("Received no predictions or empty image data from Image Generation API for prompt: {}", prompt);
                    return Mono.empty();
                })
                .doOnError(e -> log.error("Error calling Image Generation API for prompt '{}': {}", prompt, e.getMessage(), e));
    }

    private String getTextFromGeminiResponse(GeminiResponse response) {
        if (response != null && response.getFirstCandidateText() != null) {
            return response.getFirstCandidateText();
        }
        throw new IllegalStateException("Invalid GeminiResponse format or empty content.");
    }

    @lombok.Data
    private static class PromptsResponse {
        private List<String> prompts;
    }
} 