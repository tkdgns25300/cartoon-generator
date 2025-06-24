package com.sanghun.cartoon_generator.controller;

import com.sanghun.cartoon_generator.dto.PanelResult;
import com.sanghun.cartoon_generator.service.ImageGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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
        List<PanelResult> panels = imageGenerationService.generateCartoonPanels(prompt);
        model.addAttribute("panels", panels);
        model.addAttribute("originalPrompt", prompt);
        return "index";
    }
}