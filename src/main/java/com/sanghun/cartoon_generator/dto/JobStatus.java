package com.sanghun.cartoon_generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatus {
    private String jobId;
    private String status; // "PROCESSING", "COMPLETED", "FAILED"
    private String message;
    private int progress; // 0-100
    private String story;
    private String characterDescriptions;
    private List<PanelResult> results; // list of PanelResult objects
    private String errorMessage;

    public static JobStatus submitted(String jobId) {
        return JobStatus.builder()
                .jobId(jobId)
                .status("SUBMITTED")
                .progress(5)
                .build();
    }

    public static JobStatus processing(String jobId) {
        return JobStatus.builder()
                .jobId(jobId)
                .status("PROCESSING")
                .progress(10)
                .build();
    }

    public static JobStatus completed(String jobId, String story, String characterDescriptions, List<PanelResult> results) {
        return JobStatus.builder()
                .jobId(jobId)
                .status("COMPLETED")
                .progress(100)
                .story(story)
                .characterDescriptions(characterDescriptions)
                .results(results)
                .build();
    }

    public static JobStatus failed(String jobId, String errorMessage) {
        return JobStatus.builder()
                .jobId(jobId)
                .status("FAILED")
                .progress(100)
                .errorMessage(errorMessage)
                .build();
    }
} 