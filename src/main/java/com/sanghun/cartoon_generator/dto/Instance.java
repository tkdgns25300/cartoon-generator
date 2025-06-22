package com.sanghun.cartoon_generator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Instance {
    private String prompt;
    private Image image;

    public static Instance fromPrompt(String prompt) {
        return Instance.builder().prompt(prompt).build();
    }

    public static Instance fromPromptAndImage(String prompt, String imageBytes) {
        return Instance.builder()
                .prompt(prompt)
                .image(Image.builder().bytesBase64Encoded(imageBytes).build())
                .build();
    }
}