package com.sanghun.cartoon_generator.dto;

import lombok.Data;

@Data
public class CartoonGenerationRequest {
    private String story;
    private boolean includeDialogue;
} 