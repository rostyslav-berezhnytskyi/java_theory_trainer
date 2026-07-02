package com.els.javatheorytrainer.service;

import com.els.javatheorytrainer.config.AiProperties;
import com.els.javatheorytrainer.enums.AiUsageOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiTextToSpeechService {

    private static final String SPEECH_API_URL = "https://api.openai.com/v1/audio/speech";
    private static final int MAX_TEXT_LENGTH = 8000;

    private final AiProperties aiProperties;
    private final AiUsageLogService aiUsageLogService;

    public byte[] createSpeech(String text) {
        if (!isAvailable()) {
            throw new IllegalStateException("AI text-to-speech is disabled or OPENAI_API_KEY is not configured");
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text for speech is empty");
        }

        String input = limit(text.trim(), MAX_TEXT_LENGTH);

        Map<String, Object> request = Map.of(
                "model", aiProperties.getTtsModel(),
                "voice", aiProperties.getTtsVoice(),
                "input", input,
                "instructions", """
                        Speak in Ukrainian naturally and clearly.
                        Keep English Java terms readable: JVM, JDK, JRE, bytecode, javac, JIT, Garbage Collector.
                        Use a calm teaching tone and moderate speed.
                        """,
                "response_format", "mp3"
        );

        try {
            byte[] audio = buildRestClient().post()
                    .uri(SPEECH_API_URL)
                    .header("Authorization", "Bearer " + aiProperties.getOpenaiApiKey())
                    .header("Content-Type", "application/json")
                    .body(request)
                    .retrieve()
                    .body(byte[].class);

            if (audio == null || audio.length == 0) {
                throw new IllegalStateException("OpenAI text-to-speech response is empty");
            }

            aiUsageLogService.logSuccess(
                    AiUsageOperation.TEXT_TO_SPEECH,
                    aiProperties.getTtsModel(),
                    input.length(),
                    0,
                    (long) audio.length,
                    null,
                    null
            );
            return audio;
        } catch (Exception e) {
            aiUsageLogService.logFailure(
                    AiUsageOperation.TEXT_TO_SPEECH,
                    aiProperties.getTtsModel(),
                    input.length(),
                    null,
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

    private String limit(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength) + "\n[trimmed]";
    }
}
