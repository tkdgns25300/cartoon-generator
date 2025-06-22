package com.sanghun.cartoon_generator.controller;

import com.sanghun.cartoon_generator.service.ImageGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class ImageGenerationController {

    private final ImageGenerationService imageGenerationService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/generate-image")
    public String generateImage(@RequestParam("prompt") String prompt, Model model) {
        try {
            String imageBase64 = imageGenerationService.generateImage(prompt);
            model.addAttribute("generatedImage", imageBase64);
            model.addAttribute("prompt", prompt);
        } catch (Exception e) {
            model.addAttribute("error", "Error generating image: " + e.getMessage());
        }
        return "index";
    }
}