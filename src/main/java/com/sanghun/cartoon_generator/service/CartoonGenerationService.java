package com.sanghun.cartoon_generator.service;

import com.sanghun.cartoon_generator.dto.CartoonGenerationRequest;
import com.sanghun.cartoon_generator.dto.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CartoonGenerationService {

    private final CartoonGenerationWorker cartoonGenerationWorker;
    private final ConcurrentHashMap<String, JobStatus> jobStatuses;

    public String submitJob(CartoonGenerationRequest request) {
        String jobId = UUID.randomUUID().toString();
        JobStatus status = new JobStatus(jobId, false, 5, "Submitted. Waiting for process to start...", null, null, request.getStory());
        jobStatuses.put(jobId, status);
        cartoonGenerationWorker.generateCartoonPanels(jobId, request.getStory(), request.isIncludeDialogue());
        return jobId;
    }

    public JobStatus getJobStatus(String jobId) {
        return jobStatuses.get(jobId);
    }
} 