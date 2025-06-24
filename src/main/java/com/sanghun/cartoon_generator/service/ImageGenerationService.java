package com.sanghun.cartoon_generator.service;

import com.sanghun.cartoon_generator.dto.GenerationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.springframework.core.task.TaskExecutor;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationService {

    private final VertexAiService vertexAiService;
    private final TaskExecutor taskExecutor;

    public GenerationResult generateImageAndPrompts(String prompt) {
        try {
            CompletableFuture<String> imageFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return vertexAiService.generateSingleImage(prompt);
                } catch (IOException e) {
                    log.error("Error generating image", e);
                    throw new RuntimeException(e);
                }
            }, taskExecutor);

            CompletableFuture<List<String>> promptsFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return vertexAiService.generateTenPrompts(prompt);
                } catch (IOException e) {
                    log.error("Error generating prompts", e);
                    throw new RuntimeException(e);
                }
            }, taskExecutor);

            CompletableFuture.allOf(imageFuture, promptsFuture).join();

            return new GenerationResult(imageFuture.get(), promptsFuture.get());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during async generation", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to generate image and prompts", e);
        }
    }
}