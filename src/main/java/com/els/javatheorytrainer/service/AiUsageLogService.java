package com.els.javatheorytrainer.service;

import com.els.javatheorytrainer.dto.AiUsageSummary;
import com.els.javatheorytrainer.entity.AiUsageLog;
import com.els.javatheorytrainer.entity.PracticeAttempt;
import com.els.javatheorytrainer.entity.Question;
import com.els.javatheorytrainer.enums.AiUsageOperation;
import com.els.javatheorytrainer.repository.AiUsageLogRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiUsageLogService {

    private final AiUsageLogRepository aiUsageLogRepository;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(
            AiUsageOperation operation,
            String model,
            int inputChars,
            int outputChars,
            Long audioBytes,
            Long questionId,
            Long attemptId
    ) {
        save(operation, model, inputChars, outputChars, audioBytes, true, null, questionId, attemptId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(
            AiUsageOperation operation,
            String model,
            int inputChars,
            Long audioBytes,
            Long questionId,
            Long attemptId,
            Exception exception
    ) {
        save(operation, model, inputChars, 0, audioBytes, false, errorMessage(exception), questionId, attemptId);
    }

    @Transactional(readOnly = true)
    public List<AiUsageSummary> summarizeByOperationAndModel() {
        return aiUsageLogRepository.summarizeByOperationAndModel();
    }

    @Transactional(readOnly = true)
    public List<AiUsageLog> findRecentLogs(int limit) {
        return aiUsageLogRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    private void save(
            AiUsageOperation operation,
            String model,
            int inputChars,
            int outputChars,
            Long audioBytes,
            boolean success,
            String errorMessage,
            Long questionId,
            Long attemptId
    ) {
        try {
            AiUsageLog log = new AiUsageLog();
            log.setOperation(operation);
            log.setModel(model == null || model.isBlank() ? "unknown" : model.trim());
            log.setInputChars(Math.max(inputChars, 0));
            log.setOutputChars(Math.max(outputChars, 0));
            log.setAudioBytes(audioBytes);
            log.setSuccess(success);
            log.setErrorMessage(errorMessage);

            if (questionId != null) {
                log.setQuestion(entityManager.getReference(Question.class, questionId));
            }
            if (attemptId != null) {
                log.setPracticeAttempt(entityManager.getReference(PracticeAttempt.class, attemptId));
            }

            aiUsageLogRepository.save(log);
        } catch (Exception ignored) {
            // Usage logging must never break practice flow.
        }
    }

    private String errorMessage(Exception exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "AI call failed";
        }

        String message = exception.getMessage().trim();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
