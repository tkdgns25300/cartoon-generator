package com.sanghun.cartoon_generator.controller;

import com.sanghun.cartoon_generator.dto.CartoonGenerationRequest;
import com.sanghun.cartoon_generator.dto.JobStatus;
import com.sanghun.cartoon_generator.service.CartoonGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cartoon")
public class CartoonController {

    private final CartoonGenerationService cartoonGenerationService;

    @GetMapping
    public String cartoonHomePage(Model model) {
        model.addAttribute("request", new CartoonGenerationRequest());
        return "cartoon";
    }

    @PostMapping("/generate")
    public String generateCartoon(CartoonGenerationRequest request, RedirectAttributes redirectAttributes) {
        String jobId = cartoonGenerationService.submitJob(request);
        redirectAttributes.addAttribute("jobId", jobId);
        return "redirect:/cartoon/loading/{jobId}";
    }

    @GetMapping("/loading/{jobId}")
    public String loadingPage(@PathVariable String jobId, Model model) {
        model.addAttribute("jobId", jobId);
        return "loading";
    }

    @GetMapping("/api/status/{jobId}")
    @ResponseBody
    public JobStatus getStatus(@PathVariable String jobId) {
        return cartoonGenerationService.getJobStatus(jobId);
    }

    @GetMapping("/result/{jobId}")
    public String resultPage(@PathVariable String jobId, Model model) {
        JobStatus status = cartoonGenerationService.getJobStatus(jobId);
        if (status != null && status.isCompleted()) {
            model.addAttribute("originalStory", status.getOriginalStory());
            model.addAttribute("prompts", status.getPrompts());
            model.addAttribute("panelResults", status.getPanelResults());
            model.addAttribute("error", status.getMessage());
        } else if (status != null) {
            model.addAttribute("error", "Cartoon generation is still in progress. Please wait and refresh.");
        } else {
            model.addAttribute("error", "Job not found.");
        }
        return "result";
    }
} 