package com.sanghun.cartoon_generator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ImageGenerationService {

    private final VertexAiService vertexAiService;

    public String generateImage(String prompt) throws IOException {
        return vertexAiService.generateSingleImage(prompt);
    }
}