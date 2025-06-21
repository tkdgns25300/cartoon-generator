package com.sanghun.cartoon_generator.service;

import com.sanghun.cartoon_generator.dto.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartoonGenerationService {

    private final VertexAiService vertexAiService;
    private final ConcurrentHashMap<String, JobStatus> jobStatuses = new ConcurrentHashMap<>();

    public String startCartoonGeneration(String story) {
        String jobId = UUID.randomUUID().toString();
        JobStatus initialStatus = JobStatus.builder()
                .status("PROCESSING")
                .progress(0)
                .results(new ArrayList<>())
                .build();
        jobStatuses.put(jobId, initialStatus);

        generateCartoonAsync(jobId, story);
        return jobId;
    }

    @Async("taskExecutor")
    public void generateCartoonAsync(String jobId, String story) {
        try {
            log.info("Job {} started for story: {}", jobId, story.substring(0, Math.min(story.length(), 50)));

            // Step 1: Get 10 prompts from Gemini
            updateProgress(jobId, 5, "Generating prompts...");
            String promptsResponse = vertexAiService.getPromptsFromStory(story).block();
            if (promptsResponse == null || promptsResponse.isEmpty()) {
                throw new IOException("Failed to get prompts from Gemini.");
            }
            List<String> prompts = Arrays.stream(promptsResponse.split("\n"))
                    .map(line -> line.replaceAll("^\\d+\\.\\s*", ""))
                    .filter(line -> !line.trim().isEmpty())
                    .collect(Collectors.toList());

            if (prompts.size() < 1) { // Could be less than 10, that's fine
                 throw new IOException("Gemini returned no usable prompts.");
            }
             log.info("Job {} received {} prompts from Gemini.", jobId, prompts.size());
            updateProgress(jobId, 10, "Prompts generated. Starting image generation...");

            // Step 2: Generate images for each prompt
            List<String> base64Images = new ArrayList<>();
            AtomicInteger completedCount = new AtomicInteger(0);
            
            List<CompletableFuture<Void>> futures = prompts.stream().map(prompt -> 
                CompletableFuture.runAsync(() -> {
                    try {
                        String image = vertexAiService.generateImage(prompt).block();
                        if (image != null) {
                            synchronized (base64Images) {
                                base64Images.add(image);
                            }
                        }
                    } catch (IOException e) {
                        log.error("Error generating image for prompt: " + prompt, e);
                    } finally {
                        int count = completedCount.incrementAndGet();
                        int progress = 10 + (int) ((double) count / prompts.size() * 90);
                         updateProgress(jobId, progress, String.format("Generated %d/%d images...", count, prompts.size()));
                    }
                })
            ).collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();


            // Step 3: Finalize job
            JobStatus finalStatus = JobStatus.builder()
                    .status("COMPLETED")
                    .progress(100)
                    .results(base64Images)
                    .build();
            jobStatuses.put(jobId, finalStatus);
            log.info("Job {} completed successfully.", jobId);

        } catch (Exception e) {
            log.error("Job {} failed.", jobId, e);
            JobStatus failedStatus = JobStatus.builder()
                    .status("FAILED")
                    .progress(100)
                    .results(new ArrayList<>())
                    .build();
            jobStatuses.put(jobId, failedStatus);
        }
    }

    public JobStatus getJobStatus(String jobId) {
        return jobStatuses.get(jobId);
    }
    
    private void updateProgress(String jobId, int progress, String status) {
        JobStatus currentStatus = jobStatuses.get(jobId);
        if (currentStatus != null) {
            currentStatus.setProgress(progress);
            currentStatus.setStatus(status);
        }
    }
} 