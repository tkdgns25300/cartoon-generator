package com.sanghun.cartoon_generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Parameters {
    @JsonProperty("sampleCount")
    private int sampleCount;

    @JsonProperty("enhancePrompt")
    private boolean enhancePrompt;
}