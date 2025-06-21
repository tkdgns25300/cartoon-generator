package com.sanghun.cartoon_generator.controller;

import com.sanghun.cartoon_generator.dto.CartoonGenerationRequest;
import com.sanghun.cartoon_generator.dto.JobStatus;
import com.sanghun.cartoon_generator.service.CartoonGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String generateCartoon(@ModelAttribute CartoonGenerationRequest request) {
        String jobId = cartoonGenerationService.startCartoonGeneration(request.getStory());
        return "redirect:/cartoon/loading/" + jobId;
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
        if (status != null && "COMPLETED".equals(status.getStatus())) {
            model.addAttribute("images", status.getResults());
        } else {
            // Handle cases where the job is not found, not complete, or failed
            model.addAttribute("error", "Cartoon generation failed or is still in progress.");
        }
        return "result";
    }
} 