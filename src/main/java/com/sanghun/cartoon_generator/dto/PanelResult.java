package com.sanghun.cartoon_generator.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PanelResult {
    private int index;
    private String status;
    private String imageData;
} 