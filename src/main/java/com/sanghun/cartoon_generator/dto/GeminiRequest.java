package com.sanghun.cartoon_generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class GeminiRequest {

    private List<Content> contents;

    @JsonProperty("generation_config")
    private GenerationConfig generationConfig;

    public static GeminiRequest fromPrompt(String prompt) {
        Part part = Part.builder().text(prompt).build();
        Content content = Content.builder()
                .role("user")
                .parts(Collections.singletonList(part))
                .build();
        return GeminiRequest.builder()
                .contents(Collections.singletonList(content))
                .generationConfig(new GenerationConfig()) // Use default config
                .build();
    }

    @Data
    @Builder
    public static class Content {
        private String role;
        private List<Part> parts;
    }

    @Data
    @Builder
    public static class Part {
        private String text;
    }

    @Data
    public static class GenerationConfig {
        private float temperature = 1.0f;
        @JsonProperty("top_k")
        private int topK = 32;
        @JsonProperty("top_p")
        private float topP = 1.0f;
        @JsonProperty("max_output_tokens")
        private int maxOutputTokens = 8192;
    }
} 