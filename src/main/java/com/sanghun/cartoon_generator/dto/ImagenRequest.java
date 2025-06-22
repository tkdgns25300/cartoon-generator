package com.sanghun.cartoon_generator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagenRequest {
    private List<Instance> instances;
    private Parameters parameters;

    public static ImagenRequest fromInstance(Instance instance) {
        return ImagenRequest.builder()
                .instances(Collections.singletonList(instance))
                .build();
    }
}