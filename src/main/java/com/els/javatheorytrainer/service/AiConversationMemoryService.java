package com.els.javatheorytrainer.service;

import com.els.javatheorytrainer.dto.AiChatMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiConversationMemoryService {

    private static final int MAX_MESSAGES_PER_ATTEMPT = 6;

    private final Map<String, List<AiChatMessage>> conversations = new ConcurrentHashMap<>();

    public List<AiChatMessage> getHistory(String sessionId, Long attemptId) {
        return List.copyOf(conversations.getOrDefault(key(sessionId, attemptId), List.of()));
    }

    public void addExchange(String sessionId, Long attemptId, String userMessage, String assistantReply) {
        conversations.compute(key(sessionId, attemptId), (ignored, existing) -> {
            List<AiChatMessage> messages = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            messages.add(new AiChatMessage("user", userMessage));
            messages.add(new AiChatMessage("assistant", assistantReply));

            if (messages.size() > MAX_MESSAGES_PER_ATTEMPT) {
                return new ArrayList<>(messages.subList(messages.size() - MAX_MESSAGES_PER_ATTEMPT, messages.size()));
            }

            return messages;
        });
    }

    public void clearAttempt(String sessionId, Long attemptId) {
        conversations.remove(key(sessionId, attemptId));
    }

    public void clearSession(String sessionId) {
        conversations.keySet().removeIf(key -> key.startsWith(sessionId + ":"));
    }

    private String key(String sessionId, Long attemptId) {
        return sessionId + ":" + attemptId;
    }
}
