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
    private String status; // "PROCESSING", "COMPLETED", "FAILED"
    private int progress; // 0-100
    private String story;
    private String characterDescriptions;
    private List<PanelResult> results; // list of PanelResult objects
} 