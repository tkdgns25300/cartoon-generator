package com.sanghun.cartoon_generator.controller;

import com.sanghun.cartoon_generator.dto.PanelResult;
import com.sanghun.cartoon_generator.service.ImageGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ImageGenerationController {

    private final ImageGenerationService imageGenerationService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/generate")
    public SseEmitter generateImage(@RequestParam String prompt) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 minutes timeout
        imageGenerationService.generateCartoonPanelsWithProgress(prompt, emitter);
        return emitter;
    }
}