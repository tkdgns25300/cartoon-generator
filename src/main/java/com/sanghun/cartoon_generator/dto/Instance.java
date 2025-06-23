package com.sanghun.cartoon_generator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Instance {
    private String prompt;
    private Image image;

    public Instance(String prompt) {
        this.prompt = prompt;
    }
}