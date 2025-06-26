package com.sanghun.cartoon_generator.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProgressUpdate {
    private final String message;
    private final int percentage;
} 