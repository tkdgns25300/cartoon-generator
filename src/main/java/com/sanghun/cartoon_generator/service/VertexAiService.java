package com.sanghun.cartoon_generator.service;

import com.sanghun.cartoon_generator.dto.*;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Slf4j
@Service
public class VertexAiService {

    private static final String IMAGEN_API_ENDPOINT_TEMPLATE = "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predict";

    private final RestTemplate restTemplate;
    private final String projectId;
    private final String region;
    private final String imagenModelId;
    private final GoogleCredentials credentials;

    public VertexAiService(RestTemplate restTemplate,
            @Value("${google.cloud.project-id}") String projectId,
            @Value("${google.cloud.region}") String region,
            @Value("${google.cloud.imagen-model-id}") String imagenModelId) throws IOException {
        this.restTemplate = restTemplate;
        this.projectId = projectId;
        this.region = region;
        this.imagenModelId = imagenModelId;
        this.credentials = GoogleCredentials.getApplicationDefault()
                .createScoped("https://www.googleapis.com/auth/cloud-platform");
    }

    public String generateSingleImage(String prompt) throws IOException {
        log.info("Generating single image for prompt: {}", prompt);
        Instance instance = Instance.fromPrompt(prompt);
        ImagenRequest imagenRequest = ImagenRequest.fromInstance(instance);
        String url = String.format(IMAGEN_API_ENDPOINT_TEMPLATE, region, projectId, region, imagenModelId);
        ResponseEntity<ImagenResponse> response = restTemplate.postForEntity(url,
                new HttpEntity<>(imagenRequest, createHeaders()), ImagenResponse.class);

        if (response.getBody() != null && response.getBody().getPredictions() != null
                && !response.getBody().getPredictions().isEmpty()) {
            String base64Image = response.getBody().getPredictions().get(0).getBytesBase64Encoded();
            log.info("Successfully generated single image.");
            return base64Image;
        }
        throw new IOException("Failed to generate single image from Vertex AI");
    }

    private HttpHeaders createHeaders() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken());
        return headers;
    }

    private String getAccessToken() throws IOException {
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }
}