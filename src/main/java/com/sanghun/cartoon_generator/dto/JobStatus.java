package com.sanghun.cartoon_generator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobStatus {
    private String jobId;
    private boolean completed;
    private int progress;
    private String message;
    private List<PanelResult> panelResults;
    private List<String> prompts;
    private String originalStory;
} 