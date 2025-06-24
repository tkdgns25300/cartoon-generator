package com.sanghun.cartoon_generator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Instance {
    private String prompt;

    public static Instance fromPrompt(String prompt) {
        return new Instance(prompt);
    }
}