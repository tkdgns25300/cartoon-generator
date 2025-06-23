package com.sanghun.cartoon_generator.service;

import com.sanghun.cartoon_generator.dto.JobStatus;
import com.sanghun.cartoon_generator.dto.PanelResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartoonGenerationWorker {

    private final VertexAiService vertexAiService;
    private final ConcurrentHashMap<String, JobStatus> jobStatuses;

    @Async("cartoonTaskExecutor")
    public CompletableFuture<Void> generateCartoonPanels(String jobId, String story, boolean includeDialog) {
        JobStatus status = jobStatuses.get(jobId);
        try {
            log.info("Job {} started: Story - '{}', Include Dialog - {}", jobId, story, includeDialog);
            if (status == null) {
                log.error("JobStatus not found for job id: {}", jobId);
                return CompletableFuture.completedFuture(null);
            }

            status.setMessage("Generating consistent prompts...");
            status.setProgress(5);

            List<String> prompts = vertexAiService.generateConsistentPrompts(story, includeDialog).block();
            if (prompts == null || prompts.isEmpty()) {
                throw new IllegalStateException("Generated prompt list is null or empty.");
            }
            log.info("Job {}: Generated {} prompts.", jobId, prompts.size());
            status.setPrompts(prompts);
            status.setMessage("Prompts generated. Starting image generation...");
            status.setProgress(10);

            List<PanelResult> panelResults = Flux.fromIterable(prompts)
                    .parallel()
                    .runOn(Schedulers.parallel())
                    .flatMap(prompt -> {
                        int index = prompts.indexOf(prompt);
                        log.info("Job {}: Generating image for prompt {}/{}", jobId, index + 1, prompts.size());
                        return vertexAiService.generateImage(prompt)
                                .retry(2)
                                .map(imageData -> {
                                    log.info("Job {}: Successfully generated image for prompt {}/{}", jobId, index + 1, prompts.size());
                                    // Periodically update main status progress
                                    synchronized (status) {
                                        int currentProgress = status.getProgress();
                                        if (prompts.size() > 0) {
                                           status.setProgress(currentProgress + (90 / prompts.size()));
                                        }
                                    }
                                    return new PanelResult(index, "success", imageData);
                                })
                                .switchIfEmpty(Mono.fromCallable(() -> {
                                    log.warn("Job {}: Image generation for prompt {}/{} failed after retries.", jobId, index + 1, prompts.size());
                                    return new PanelResult(index, "failed", null);
                                }))
                                .doOnError(e -> log.error("Job {}: Error generating image for prompt: {}", jobId, prompt, e))
                                .onErrorResume(e -> Mono.just(new PanelResult(index, "failed", null)));
                    })
                    .sequential()
                    .collectList()
                    .block();

            if (panelResults == null) {
                throw new IllegalStateException("Panel generation resulted in null list.");
            }

            panelResults.sort(java.util.Comparator.comparingInt(PanelResult::getIndex));

            long failedCount = panelResults.stream().filter(p -> "failed".equals(p.getStatus())).count();
            if (failedCount > 0) {
                 log.warn("Job {} finished with {} failed images.", jobId, failedCount);
                 status.setMessage(String.format("Generation complete with %d error(s).", failedCount));
            } else {
                 log.info("Job {} completed successfully.", jobId);
                 status.setMessage("Generation complete!");
            }

            status.setProgress(100);
            status.setCompleted(true);
            status.setPanelResults(panelResults);

        } catch (Exception e) {
            log.error("Error during cartoon generation for job id: {}", jobId, e);
            if (status != null) {
                status.setCompleted(true);
                status.setProgress(100);
                status.setMessage("Failed: " + e.getMessage());
            }
        }
        return CompletableFuture.completedFuture(null);
    }
}