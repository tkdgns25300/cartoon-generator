package com.sanghun.cartoon_generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prediction {
    @JsonProperty("bytesBase64Encoded")
    private String bytesBase64Encoded;

    @JsonProperty("mimeType")
    private String mimeType;
}