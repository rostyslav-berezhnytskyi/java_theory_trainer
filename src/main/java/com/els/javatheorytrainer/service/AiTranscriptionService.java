package com.els.javatheorytrainer.service;

import com.els.javatheorytrainer.config.AiProperties;
import com.els.javatheorytrainer.enums.AiUsageOperation;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AiTranscriptionService {

    private static final String TRANSCRIPTIONS_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    private final AiProperties aiProperties;
    private final AiUsageLogService aiUsageLogService;

    public String transcribe(MultipartFile audioFile) {
        if (!isAvailable()) {
            throw new IllegalStateException("AI transcription is disabled or OPENAI_API_KEY is not configured");
        }

        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("Audio file is empty");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("model", aiProperties.getTranscriptionModel());
        body.add("language", "uk");
        body.add("prompt", """
                Це відповідь українською на Java interview question.
                Часто трапляються англійські технічні терміни:
                JVM, JDK, JRE, bytecode, machine code, .java, .class, javac,
                class loader, Garbage Collector, JIT compiler, heap, stack.
                """);
        body.add("file", audioResource(audioFile));

        try {
            JsonNode response = buildRestClient().post()
                    .uri(TRANSCRIPTIONS_API_URL)
                    .header("Authorization", "Bearer " + aiProperties.getOpenaiApiKey())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String text = response == null ? null : response.path("text").asText(null);
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("OpenAI transcription response does not contain text");
            }

            String trimmedText = text.trim();
            aiUsageLogService.logSuccess(
                    AiUsageOperation.TRANSCRIPTION,
                    aiProperties.getTranscriptionModel(),
                    0,
                    trimmedText.length(),
                    audioFile.getSize(),
                    null,
                    null
            );
            return trimmedText;
        } catch (Exception e) {
            aiUsageLogService.logFailure(
                    AiUsageOperation.TRANSCRIPTION,
                    aiProperties.getTranscriptionModel(),
                    0,
                    audioFile.getSize(),
                    null,
                    null,
                    e
            );
            throw e;
        }
    }

    private boolean isAvailable() {
        return aiProperties.isEnabled()
                && aiProperties.getOpenaiApiKey() != null
                && !aiProperties.getOpenaiApiKey().isBlank();
    }

    private RestClient buildRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private ByteArrayResource audioResource(MultipartFile audioFile) {
        try {
            byte[] bytes = audioFile.getBytes();

            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    String originalFilename = audioFile.getOriginalFilename();
                    return originalFilename == null || originalFilename.isBlank()
                            ? "practice-answer.webm"
                            : originalFilename;
                }
            };
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded audio", e);
        }
    }
}
