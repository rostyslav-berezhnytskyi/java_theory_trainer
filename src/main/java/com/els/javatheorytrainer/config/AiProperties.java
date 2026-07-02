package com.els.javatheorytrainer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiProperties {

    private final boolean enabled;
    private final String openaiApiKey;
    private final String model;
    private final String transcriptionModel;
    private final String ttsModel;
    private final String ttsVoice;
    private final int chatMaxOutputTokens;
    private final int timeoutSeconds;

    public AiProperties(@Value("${app.ai.enabled:true}") boolean enabled,
                        @Value("${app.ai.openai-api-key:}") String openaiApiKey,
                        @Value("${app.ai.model:gpt-5.4-mini}") String model,
                        @Value("${app.ai.transcription-model:gpt-4o-transcribe}") String transcriptionModel,
                        @Value("${app.ai.tts-model:gpt-4o-mini-tts}") String ttsModel,
                        @Value("${app.ai.tts-voice:marin}") String ttsVoice,
                        @Value("${app.ai.chat-max-output-tokens:3000}") int chatMaxOutputTokens,
                        @Value("${app.ai.timeout-seconds:45}") int timeoutSeconds) {
        this.enabled = enabled;
        this.openaiApiKey = openaiApiKey;
        this.model = model;
        this.transcriptionModel = transcriptionModel;
        this.ttsModel = ttsModel;
        this.ttsVoice = ttsVoice;
        this.chatMaxOutputTokens = chatMaxOutputTokens;
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    public String getModel() {
        return model;
    }

    public String getTranscriptionModel() {
        return transcriptionModel;
    }

    public String getTtsModel() {
        return ttsModel;
    }

    public String getTtsVoice() {
        return ttsVoice;
    }

    public int getChatMaxOutputTokens() {
        return chatMaxOutputTokens;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
