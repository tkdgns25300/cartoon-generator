package com.sanghun.cartoon_generator.service;

import com.sanghun.cartoon_generator.dto.PanelResult;
import com.sanghun.cartoon_generator.dto.ProgressUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Async("taskExecutor")
    public void generateCartoonPanelsWithProgress(String storyIdea, SseEmitter emitter) {
        try {
            // 1. Generate story prompts
            emitter.send(SseEmitter.event().name("progress").data(new ProgressUpdate("AI is creating a story...", 5)));
            List<String> prompts = vertexAiService.generateStoryPrompts(storyIdea);

            if (prompts.isEmpty() || prompts.stream().allMatch(String::isEmpty)) {
                log.warn("No prompts were generated, aborting image generation.");
                emitter.send(SseEmitter.event().name("error").data("Failed to generate a story. Please try a different idea."));
                return;
            }
            emitter.send(SseEmitter.event().name("progress").data(new ProgressUpdate("Story created! Generating 10 cartoon panels...", 10)));

            // 2. Generate images in parallel and send updates
            final int totalPanels = prompts.size();
            final AtomicInteger completedPanels = new AtomicInteger(0);

            List<CompletableFuture<Void>> panelFutures = prompts.stream()
                    .map(prompt -> CompletableFuture.supplyAsync(() -> {
                        try {
                            String image = vertexAiService.generateSingleImage(prompt);
                            return new PanelResult(prompt, image);
                        } catch (IOException e) {
                            log.error("Failed to generate image for prompt: {}", prompt, e);
                            return new PanelResult(prompt, null); // Return with null image on failure
                        }
                    }, taskExecutor)
                    .thenAcceptAsync(panelResult -> {
                        try {
                            int completed = completedPanels.incrementAndGet();
                            int percentage = 10 + (int) ((double) completed / totalPanels * 90);
                            emitter.send(SseEmitter.event().name("progress").data(new ProgressUpdate("Generated panel " + completed + "/" + totalPanels, percentage)));
                            emitter.send(SseEmitter.event().name("panel").data(panelResult));
                        } catch (IOException e) {
                            log.warn("Failed to send SSE event for a panel.", e);
                        }
                    }, taskExecutor))
                    .collect(Collectors.toList());

            // 3. Wait for all futures to complete
            CompletableFuture.allOf(panelFutures.toArray(new CompletableFuture[0])).join();

            // 4. Send completion event
            emitter.send(SseEmitter.event().name("complete").data("Cartoon generation complete!"));

        } catch (Exception e) {
            log.error("Error during cartoon generation for SSE stream", e);
            try {
                emitter.send(SseEmitter.event().name("error").data("An unexpected error occurred: " + e.getMessage()));
            } catch (IOException ioException) {
                log.warn("Failed to send SSE error event.", ioException);
            }
        } finally {
            emitter.complete();
        }
    }
}