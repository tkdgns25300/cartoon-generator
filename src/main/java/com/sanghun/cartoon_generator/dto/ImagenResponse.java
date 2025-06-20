package com.sanghun.cartoon_generator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImagenResponse {
    private List<Prediction> predictions;
}