package com.sanghun.cartoon_generator.controller;

import com.sanghun.cartoon_generator.service.RandomNumberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RandomNumberController {

    private final RandomNumberService randomNumberService;

    @GetMapping("/")
    public String showIndexPage() {
        return "index";
    }

    @PostMapping("/generate")
    public String generateNumber(Model model) {
        int number = randomNumberService.generateRandomNumber();
        model.addAttribute("randomNumber", number);
        return "index";
    }
} 