package com.sanghun.cartoon_generator.service;

import com.sanghun.cartoon_generator.dto.JobStatus;
import com.sanghun.cartoon_generator.dto.PanelResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartoonGenerationService {

    private final CartoonGenerationWorker cartoonGenerationWorker;
    private final ConcurrentHashMap<String, JobStatus> jobStatuses = new ConcurrentHashMap<>();

    public String startCartoonGeneration(String story, boolean includeDialogue) {
        String jobId = UUID.randomUUID().toString();
        JobStatus initialStatus = JobStatus.builder()
                .status("STARTING")
                .progress(0)
                .story(story)
                .results(Collections.emptyList())
                .build();
        jobStatuses.put(jobId, initialStatus);

        log.info("Job {} submitted. Offloading to async worker.", jobId);
        cartoonGenerationWorker.generateCartoonAsync(jobId, story, includeDialogue);
        return jobId;
    }

    public JobStatus getJobStatus(String jobId) {
        return jobStatuses.get(jobId);
    }

    public void updateStatus(String jobId, String statusMessage, int progress, List<PanelResult> currentResults) {
        JobStatus currentStatus = jobStatuses.get(jobId);
        if (currentStatus != null && !"COMPLETED".equals(currentStatus.getStatus()) && !"FAILED".equals(currentStatus.getStatus())) {
            jobStatuses.put(jobId, JobStatus.builder()
                    .status(statusMessage)
                    .progress(progress)
                    .story(currentStatus.getStory())
                    .results(currentResults != null ? currentResults : currentStatus.getResults())
                    .build());
        }
    }

    public void updateCharacterDescriptions(String jobId, String characterDescriptions) {
        JobStatus currentStatus = jobStatuses.get(jobId);
        if (currentStatus != null) {
            currentStatus.setCharacterDescriptions(characterDescriptions);
        }
    }

    public void finalizeJob(String jobId, String status, List<PanelResult> results) {
        JobStatus currentStatus = jobStatuses.get(jobId);
        if (currentStatus == null) {
            log.error("Cannot finalize a job that does not exist: {}", jobId);
            return;
        }

        JobStatus finalStatus = JobStatus.builder()
                .status(status)
                .progress(100)
                .story(currentStatus.getStory())
                .results(results != null ? results : Collections.emptyList())
                .build();
        jobStatuses.put(jobId, finalStatus);
    }
} 