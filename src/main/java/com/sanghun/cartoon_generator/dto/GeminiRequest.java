package com.sanghun.cartoon_generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequest {

    private List<Content> contents;

    @JsonProperty("generation_config")
    private GenerationConfig generationConfig;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private String role;
        private List<Part> parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationConfig {
        private float temperature = 1.0f;
        @JsonProperty("top_k")
        private int topK = 32;
        @JsonProperty("top_p")
        private float topP = 1.0f;
        @JsonProperty("max_output_tokens")
        private int maxOutputTokens = 8192;
        @JsonProperty("response_mime_type")
        private String responseMimeType;
    }
} 