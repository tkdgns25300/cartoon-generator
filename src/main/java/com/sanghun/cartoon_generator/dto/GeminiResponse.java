package com.sanghun.cartoon_generator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GeminiResponse {
    private List<Candidate> candidates;

    public String getFirstCandidateText() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate firstCandidate = candidates.get(0);
            if (firstCandidate.getContent() != null && firstCandidate.getContent().getParts() != null
                    && !firstCandidate.getContent().getParts().isEmpty()) {
                return firstCandidate.getContent().getParts().get(0).getText();
            }
        }
        return null;
    }

    @Data
    @NoArgsConstructor
    public static class Candidate {
        private Content content;
    }

    @Data
    @NoArgsConstructor
    public static class Content {
        private String role;
        private List<Part> parts;
    }

    @Data
    @NoArgsConstructor
    public static class Part {
        private String text;
    }
}