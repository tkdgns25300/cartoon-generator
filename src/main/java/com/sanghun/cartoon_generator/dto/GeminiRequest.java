package com.sanghun.cartoon_generator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GeminiRequest {
    private List<Content> contents;

    public static GeminiRequest fromPrompt(String prompt) {
        Part part = new Part(prompt);
        Content content = new Content("user", List.of(part));
        return new GeminiRequest(List.of(content));
    }
}

@Data
@AllArgsConstructor
class Content {
    private String role;
    private List<Part> parts;
}

@Data
@AllArgsConstructor
class Part {
    private String text;
}