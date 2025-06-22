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
    private final ConcurrentHashMap<String, JobStatus> jobStatuses = new ConcurrentHashMap<>();

    public String submitJob(CartoonGenerationRequest request) {
        String jobId = UUID.randomUUID().toString();
        jobStatuses.put(jobId, JobStatus.submitted(jobId));
        cartoonGenerationWorker.generateCartoonAsync(jobId, request.getStory(), request.isIncludeDialogue());
        return jobId;
    }

    public JobStatus getJobStatus(String jobId) {
        return jobStatuses.get(jobId);
    }
} 