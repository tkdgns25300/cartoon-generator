package com.sanghun.cartoon_generator.service;

import com.sanghun.cartoon_generator.dto.PanelResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationService {

    private final VertexAiService vertexAiService;
    private final TaskExecutor taskExecutor;

    public List<PanelResult> generateCartoonPanels(String storyIdea) {
        // 1. Generate 10 story prompts first
        List<String> prompts;
        try {
            prompts = vertexAiService.generateStoryPrompts(storyIdea);
        } catch (IOException e) {
            log.error("Failed to generate story prompts", e);
            return Collections.emptyList();
        }

        if (prompts.isEmpty()) {
            log.warn("No prompts were generated, aborting image generation.");
            return Collections.emptyList();
        }

        // 2. Generate images for each prompt in parallel
        List<CompletableFuture<PanelResult>> panelFutures = prompts.stream()
                .map(prompt -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String image = vertexAiService.generateSingleImage(prompt);
                        return new PanelResult(prompt, image);
                    } catch (IOException e) {
                        log.error("Failed to generate image for prompt: {}", prompt, e);
                        return new PanelResult(prompt, null); // Return with null image on failure
                    }
                }, taskExecutor))
                .collect(Collectors.toList());

        // 3. Wait for all futures to complete and collect the results
        return panelFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }
}