package com.sanghun.cartoon_generator.dto;

import lombok.Value;

import java.util.List;

@Value
public class GenerationResult {
    String base64Image;
    List<String> generatedPrompts;
}