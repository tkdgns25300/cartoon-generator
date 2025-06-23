package com.sanghun.cartoon_generator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImagenRequest {
    private List<Instance> instances;
    private Parameters parameters;

    public ImagenRequest(Instance instance, Parameters parameters) {
        this.instances = Collections.singletonList(instance);
        this.parameters = parameters;
    }
}