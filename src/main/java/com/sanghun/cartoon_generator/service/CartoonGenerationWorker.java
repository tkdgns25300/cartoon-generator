package com.sanghun.cartoon_generator.service;

import com.sanghun.cartoon_generator.dto.CartoonGenerationRequest;
import com.sanghun.cartoon_generator.dto.JobStatus;
import com.sanghun.cartoon_generator.dto.PanelResult;
import com.sanghun.cartoon_generator.dto.Prediction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class CartoonGenerationWorker {

    private final VertexAiService vertexAiService;
    private final Map<String, JobStatus> jobStatuses;
    private final Executor cartoonTaskExecutor;

    public CartoonGenerationWorker(VertexAiService vertexAiService, Map<String, JobStatus> jobStatuses, @Qualifier("cartoonTaskExecutor") Executor cartoonTaskExecutor) {
        this.vertexAiService = vertexAiService;
        this.jobStatuses = jobStatuses;
        this.cartoonTaskExecutor = cartoonTaskExecutor;
    }

    private void updateJobProgress(String jobId, String message, int progress) {
        JobStatus currentStatus = jobStatuses.get(jobId);
        if (currentStatus != null) {
            currentStatus.setStatus("PROCESSING");
            currentStatus.setMessage(message);
            currentStatus.setProgress(progress);
        }
    }

    @Async("cartoonTaskExecutor")
    public void generateCartoonAsync(String jobId, String story, boolean includeDialogue) {
        try {
            jobStatuses.put(jobId, JobStatus.builder().jobId(jobId).status("SUBMITTED").progress(5).message("Job submitted").build());
            log.info("Job {} async process started.", jobId);

            updateJobProgress(jobId, "Generating character descriptions", 10);
            String characterDescriptions = vertexAiService.getCharacterDescriptions(story);
            log.info("Job {}: Generated character descriptions.", jobId);

            updateJobProgress(jobId, "Generating character reference image", 25);
            String referenceImageBase64 = vertexAiService.generateCharacterReferenceImage(characterDescriptions);
            log.info("Job {}: Generated character reference image.", jobId);


            updateJobProgress(jobId, "Generating panel prompts", 40);
            List<String> panelPrompts = vertexAiService.getPanelPrompts(story, characterDescriptions, includeDialogue);
            log.info("Job {}: Generated {} panel prompts.", jobId, panelPrompts.size());


            int totalPanels = panelPrompts.size();
            AtomicInteger panelsDone = new AtomicInteger(0);

            List<CompletableFuture<PanelResult>> futures = IntStream.range(0, totalPanels)
                    .mapToObj(i -> {
                        String prompt = panelPrompts.get(i);
                        return CompletableFuture.supplyAsync(() -> {
                            try {
                                log.info("Job {}: Generating image for panel {} with prompt: '{}'", jobId, i + 1, prompt);
                                String imageBase64 = vertexAiService.generateImageFromPromptAndReference(prompt, referenceImageBase64);
                                int doneCount = panelsDone.incrementAndGet();
                                int progress = 40 + (int) ((double) doneCount / totalPanels * 60);
                                updateJobProgress(jobId, String.format("Generated panel %d of %d", doneCount, totalPanels), progress);
                                log.info("Job {}: Successfully generated image for panel {}.", jobId, i + 1);
                                return new PanelResult(prompt, imageBase64);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }, cartoonTaskExecutor);
                    })
                    .collect(Collectors.toList());

            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            List<PanelResult> panelResults = allOf.thenApply(v ->
                    futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList())
            ).get();


            jobStatuses.put(jobId, JobStatus.completed(jobId, story, characterDescriptions, panelResults));
            log.info("Job {} completed successfully.", jobId);

        } catch (Exception e) {
            String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            jobStatuses.put(jobId, JobStatus.failed(jobId, errorMessage));
            log.error("Job {} failed in async worker.", jobId, e);
        }
    }
}