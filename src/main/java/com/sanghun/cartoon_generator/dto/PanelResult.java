package com.sanghun.cartoon_generator.dto;

import lombok.Value;

@Value
public class PanelResult {
    String prompt;
    String base64Image;
}