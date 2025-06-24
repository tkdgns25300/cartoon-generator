package com.sanghun.cartoon_generator.controller;

import com.sanghun.cartoon_generator.service.ImageGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ImageGenerationController {

    private final ImageGenerationService imageGenerationService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/generate")
    public String generateImage(@RequestParam String prompt, Model model) {
        String base64Image = imageGenerationService.generateImage(prompt);
        model.addAttribute("image", "data:image/png;base64," + base64Image);
        model.addAttribute("prompt", prompt);
        return "index";
    }
}