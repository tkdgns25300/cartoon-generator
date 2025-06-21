package com.sanghun.cartoon_generator.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GeminiRequest {

    private final Content contents;
    private final GenerationConfig generationConfig;

    @Getter
    @AllArgsConstructor
    public static class Content {
        private final String role;
        private final List<Part> parts;
    }

    @Getter
    @AllArgsConstructor
    public static class Part {
        private final String text;
    }

    @Getter
    @AllArgsConstructor
    public static class GenerationConfig {
        private final float temperature;
        private final int topK;
        private final int topP;
        private final int maxOutputTokens;
        private final List<String> stopSequences;
    }
} 