package com.els.javatheorytrainer.service;

import com.els.javatheorytrainer.config.AiProperties;
import com.els.javatheorytrainer.dto.AiChatMessage;
import com.els.javatheorytrainer.dto.AiEvaluationResult;
import com.els.javatheorytrainer.entity.PracticeAttempt;
import com.els.javatheorytrainer.entity.Question;
import com.els.javatheorytrainer.entity.QuestionImage;
import com.els.javatheorytrainer.enums.AiUsageOperation;
import com.els.javatheorytrainer.enums.PracticeGrade;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiEvaluationService {

    private static final String RESPONSES_API_URL = "https://api.openai.com/v1/responses";

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final AiUsageLogService aiUsageLogService;

    public boolean isAvailable() {
        return aiProperties.isEnabled() && aiProperties.getOpenaiApiKey() != null && !aiProperties.getOpenaiApiKey().isBlank();
    }

    public AiEvaluationResult evaluate(PracticeAttempt attempt) {
        if (!isAvailable()) {
            throw new IllegalStateException("AI evaluation is disabled or OPENAI_API_KEY is not configured");
        }

        Question question = attempt.getQuestion();
        String prompt = buildPrompt(question, attempt.getUserAnswer());
        int inputChars = instructions().length() + prompt.length();

        Map<String, Object> request = Map.of(
                "model", aiProperties.getModel(),
                "instructions", instructions(),
                "input", prompt,
                "reasoning", Map.of("effort", "low"),
                "max_output_tokens", 1200
        );

        try {
            String outputText = callResponsesApi(request);
            aiUsageLogService.logSuccess(
                    AiUsageOperation.EVALUATION,
                    aiProperties.getModel(),
                    inputChars,
                    outputText.length(),
                    null,
                    question.getId(),
                    attempt.getId()
            );
            return parseResult(outputText);
        } catch (Exception e) {
            aiUsageLogService.logFailure(
                    AiUsageOperation.EVALUATION,
                    aiProperties.getModel(),
                    inputChars,
                    null,
                    question.getId(),
                    attempt.getId(),
                    e
            );
            throw e;
        }
    }

    public String chat(PracticeAttempt attempt, List<AiChatMessage> history, String userMessage) {
        if (!isAvailable()) {
            throw new IllegalStateException("AI chat is disabled or OPENAI_API_KEY is not configured");
        }

        String prompt = buildChatPrompt(attempt, history, userMessage);
        int inputChars = chatInstructions().length() + prompt.length();

        Map<String, Object> request = Map.of(
                "model", aiProperties.getModel(),
                "instructions", chatInstructions(),
                "input", prompt,
                "reasoning", Map.of("effort", "low"),
                "max_output_tokens", aiProperties.getChatMaxOutputTokens()
        );

        try {
            String outputText = callResponsesApi(request);
            aiUsageLogService.logSuccess(
                    AiUsageOperation.CHAT,
                    aiProperties.getModel(),
                    inputChars,
                    outputText.length(),
                    null,
                    attempt.getQuestion().getId(),
                    attempt.getId()
            );
            return outputText;
        } catch (Exception e) {
            aiUsageLogService.logFailure(
                    AiUsageOperation.CHAT,
                    aiProperties.getModel(),
                    inputChars,
                    null,
                    attempt.getQuestion().getId(),
                    attempt.getId(),
                    e
            );
            throw e;
        }
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

    private String callResponsesApi(Map<String, Object> request) {
        JsonNode response = buildRestClient().post()
                .uri(RESPONSES_API_URL)
                .header("Authorization", "Bearer " + aiProperties.getOpenaiApiKey())
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        return extractOutputText(response);
    }

    private String instructions() {
        return """
                You are an assistant for checking Java theory practice answers.
                Evaluate the user's answer against the reference material.
                Be strict but helpful. Reply in Ukrainian, keeping Java terms in English when natural.
                Return only valid JSON without Markdown fences.
                JSON fields:
                scorePercent: integer from 0 to 100,
                suggestedGrade: one of AGAIN, HARD, GOOD, EASY,
                feedback: short summary,
                details: detailed explanation,
                missingPoints: missing important points,
                wrongParts: incorrect or misleading parts,
                goodParts: correct parts,
                followUpSuggestion: one useful follow-up question the student could ask.
                """;
    }

    private String chatInstructions() {
        return """
                You are a Java theory tutor.
                Continue discussion about one already answered practice question.
                Reply in Ukrainian, keeping Java terms in English when natural.
                Be concise, practical, and explain why the concept works this way.
                Prefer complete answers over many details. Do not stop mid-sentence.
                Do not re-grade the whole answer unless the student asks.
                """;
    }

    private String buildPrompt(Question question, String userAnswer) {
        return """
                Question:
                %s

                User answer:
                %s

                Short reference answer:
                %s

                Full reference answer:
                %s

                Theory notes:
                %s

                Must-have points:
                %s

                Common mistakes:
                %s

                Image context:
                %s
                """.formatted(
                safe(question.getQuestionText()),
                safe(userAnswer),
                safe(question.getShortAnswer()),
                safe(question.getFullAnswer()),
                limit(safe(question.getTheoryNotes()), 4000),
                formatList(question.getMustHavePoints()),
                formatList(question.getCommonMistakes()),
                formatImages(question.getImages())
        );
    }

    private String buildChatPrompt(PracticeAttempt attempt, List<AiChatMessage> history, String userMessage) {
        Question question = attempt.getQuestion();

        return """
                Practice question:
                %s

                User original answer:
                %s

                Short reference answer:
                %s

                Full reference answer:
                %s

                Theory notes:
                %s

                Must-have points:
                %s

                Common mistakes:
                %s

                AI evaluation summary:
                Score: %s
                Suggested grade: %s
                Feedback: %s
                Missing points: %s
                Wrong or unclear parts: %s
                Good parts: %s

                Recent conversation:
                %s

                Student new message:
                %s
                """.formatted(
                safe(question.getQuestionText()),
                safe(attempt.getUserAnswer()),
                safe(question.getShortAnswer()),
                safe(question.getFullAnswer()),
                limit(safe(question.getTheoryNotes()), 3000),
                formatList(question.getMustHavePoints()),
                formatList(question.getCommonMistakes()),
                attempt.getAiScorePercent() == null ? "-" : attempt.getAiScorePercent(),
                attempt.getAiSuggestedGrade() == null ? "-" : attempt.getAiSuggestedGrade(),
                safe(attempt.getAiFeedback()),
                safe(attempt.getAiMissingPoints()),
                safe(attempt.getAiWrongParts()),
                safe(attempt.getAiGoodParts()),
                formatHistory(history),
                safe(userMessage)
        );
    }

    private AiEvaluationResult parseResult(String outputText) {
        try {
            JsonNode root = objectMapper.readTree(stripJsonFence(outputText));

            return new AiEvaluationResult(
                    nullableInt(root, "scorePercent"),
                    parseGrade(root.path("suggestedGrade").asText(null)),
                    text(root, "feedback"),
                    text(root, "details"),
                    text(root, "missingPoints"),
                    text(root, "wrongParts"),
                    text(root, "goodParts"),
                    text(root, "followUpSuggestion")
            );
        } catch (Exception e) {
            throw new IllegalStateException("AI returned an invalid evaluation format", e);
        }
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("OpenAI response is empty");
        }

        String outputText = response.path("output_text").asText(null);
        if (outputText != null && !outputText.isBlank()) {
            return outputText;
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                String contentText = content.path("text").asText(null);
                if (contentText != null && !contentText.isBlank()) {
                    text.append(contentText).append('\n');
                }
            }
        }

        if (text.isEmpty()) {
            throw new IllegalStateException("OpenAI response does not contain text output");
        }

        return text.toString().trim();
    }

    private PracticeGrade parseGrade(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return PracticeGrade.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Integer nullableInt(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (node.isInt()) {
            return node.asInt();
        }
        if (node.isTextual()) {
            try {
                return Integer.parseInt(node.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String text(JsonNode root, String field) {
        String value = root.path(field).asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String stripJsonFence(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```json") && trimmed.endsWith("```")) {
            return trimmed.substring(7, trimmed.length() - 3).trim();
        }
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            return trimmed.substring(3, trimmed.length() - 3).trim();
        }
        return trimmed;
    }

    private String formatList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            result.append(i + 1).append(". ").append(safe(values.get(i))).append('\n');
        }
        return result.toString().trim();
    }

    private String formatImages(List<QuestionImage> images) {
        if (images == null || images.isEmpty()) {
            return "-";
        }

        StringBuilder result = new StringBuilder();
        for (QuestionImage image : images) {
            result.append("- ")
                    .append(image.getRole())
                    .append(": ")
                    .append(safe(image.getCaption()));

            if (image.getAltText() != null && !image.getAltText().isBlank()) {
                result.append(" Description: ").append(image.getAltText().trim());
            }

            result.append('\n');
        }
        return result.toString().trim();
    }

    private String formatHistory(List<AiChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "-";
        }

        StringBuilder result = new StringBuilder();
        for (AiChatMessage message : history) {
            result.append(message.role()).append(": ").append(safe(message.text())).append('\n');
        }
        return result.toString().trim();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String limit(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength) + "\n[trimmed]";
    }
}
