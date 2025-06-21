package com.sanghun.cartoon_generator.service;

import com.sanghun.cartoon_generator.dto.PanelResult;
import com.sanghun.cartoon_generator.dto.Prediction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartoonGenerationWorker {

    private final VertexAiService vertexAiService;
    private final CartoonGenerationService cartoonGenerationService;

    // Use @Lazy to resolve circular dependency
    public CartoonGenerationWorker(VertexAiService vertexAiService, @Lazy CartoonGenerationService cartoonGenerationService) {
        this.vertexAiService = vertexAiService;
        this.cartoonGenerationService = cartoonGenerationService;
    }

    @Async("taskExecutor")
    public void generateCartoonAsync(String jobId, String story, boolean includeDialogue) {
        try {
            log.info("Job {} async process started.", jobId);

            // Step 1: Generate Character Descriptions
            cartoonGenerationService.updateStatus(jobId, "1/3: Creating character concepts...", 10, null);
            String characterDescriptions = vertexAiService.getCharacterDescriptions(story).block();
            if (characterDescriptions == null || characterDescriptions.isEmpty()) {
                throw new IOException("Failed to generate character descriptions.");
            }
            cartoonGenerationService.updateCharacterDescriptions(jobId, characterDescriptions);

            // Step 2: Generate Prompts for each panel
            cartoonGenerationService.updateStatus(jobId, "2/3: Generating prompts for each scene...", 25, null);
            String promptsResponse = vertexAiService.getPromptsFromStory(story, characterDescriptions, includeDialogue).block();
            if (promptsResponse == null || promptsResponse.isEmpty()) {
                throw new IOException("Failed to get prompts from Gemini.");
            }
            List<String> prompts = Arrays.stream(promptsResponse.split("\n"))
                    .map(String::trim)
                    .filter(line -> line.matches("^\\d+\\.\\s.*"))
                    .map(line -> line.replaceAll("^\\d+\\.\\s*", ""))
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());

            if (prompts.isEmpty()) {
                throw new IOException("Gemini returned no usable prompts.");
            }
            cartoonGenerationService.updateStatus(jobId, "3/3: Generating cartoon panels...", 40, null);

            // Step 3: Generate Cartoon Panels
            AtomicInteger completedCount = new AtomicInteger(0);

            List<CompletableFuture<PanelResult>> panelFutures = prompts.stream().map(prompt ->
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            String image = vertexAiService.generateImage(prompt).block();
                            if (image != null) {
                                return new PanelResult(prompt, image);
                            }
                        } catch (IOException e) {
                            log.error("Error generating image for prompt: " + prompt, e);
                        } finally {
                            int count = completedCount.incrementAndGet();
                            int progress = 40 + (int) ((double) count / prompts.size() * 55);
                            cartoonGenerationService.updateStatus(jobId, String.format("3/3: Generated %d/%d panels...", count, prompts.size()), progress, null);
                        }
                        return null;
                    }, taskExecutor())
            ).collect(Collectors.toList());

            List<PanelResult> panelResults = panelFutures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            cartoonGenerationService.finalizeJob(jobId, "COMPLETED", panelResults);

        } catch (Exception e) {
            log.error("Job {} failed in async worker.", jobId, e);
            cartoonGenerationService.finalizeJob(jobId, "FAILED", null);
        }
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("CartoonGen-");
        executor.initialize();
        return executor;
    }
} 