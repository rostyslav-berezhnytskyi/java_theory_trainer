package com.els.javatheorytrainer.service;

import com.els.javatheorytrainer.dto.AiEvaluationResult;
import com.els.javatheorytrainer.dto.PracticeSessionSettings;
import com.els.javatheorytrainer.entity.PracticeAttempt;
import com.els.javatheorytrainer.entity.Question;
import com.els.javatheorytrainer.enums.PracticeQuestionFilter;
import com.els.javatheorytrainer.enums.PracticeGrade;
import com.els.javatheorytrainer.enums.PracticeScope;
import com.els.javatheorytrainer.enums.QuestionStatus;
import com.els.javatheorytrainer.repository.PracticeAttemptRepository;
import com.els.javatheorytrainer.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Service for practice mode.
 *
 * For now this is simple self-practice:
 * - pick active question;
 * - show question;
 * - show reference answer;
 * - user grades himself;
 * - update statistics.
 */
@Service
@RequiredArgsConstructor
public class PracticeService {

    private final QuestionRepository questionRepository;
    private final PracticeAttemptRepository practiceAttemptRepository;
    private final AiEvaluationService aiEvaluationService;

    /**
     * Picks next question from selected section.
     *
     * Logic:
     * - only ACTIVE questions;
     * - avoid the previous question if possible;
     * - prefer questions shown fewer times;
     * - among them prefer questions with more wrong answers.
     */
    @Transactional
    public Question pickNextQuestion(Long sectionId, Long excludeQuestionId) {
        return pickNextQuestion(PracticeSessionSettings.section(sectionId), excludeQuestionId);
    }

    @Transactional
    public Question pickNextQuestion(PracticeSessionSettings settings, Long excludeQuestionId) {
        if (settings == null) {
            throw new IllegalArgumentException("Practice settings are required");
        }

        List<Question> candidates = settings.scope() == PracticeScope.VOLUME
                ? questionRepository.findByStatusAndSectionVolumeId(QuestionStatus.ACTIVE, settings.volumeId())
                : questionRepository.findByStatusAndSectionId(QuestionStatus.ACTIVE, settings.sectionId());

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No active questions in selected practice scope");
        }

        if (excludeQuestionId != null && candidates.size() > 1) {
            candidates = candidates.stream()
                    .filter(question -> !question.getId().equals(excludeQuestionId))
                    .toList();
        }

        candidates = candidates.stream()
                .filter(question -> matchesFilter(question, settings.filter()))
                .toList();

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No active questions match selected practice filter");
        }

        candidates = new java.util.ArrayList<>(candidates);
        Collections.shuffle(candidates);

        if (!settings.randomOrder()) {
            candidates.sort(
                    Comparator.comparingInt(Question::getTimesShown)
                            .thenComparing(Comparator.comparingInt(Question::getWrongTotalCount).reversed())
                            .thenComparing(Question::getId)
            );
        }

        Question question = candidates.getFirst();
        question.markAsShown();

        return question;
    }

    private boolean matchesFilter(Question question, PracticeQuestionFilter filter) {
        PracticeQuestionFilter effectiveFilter = filter == null ? PracticeQuestionFilter.ALL_ACTIVE : filter;

        return switch (effectiveFilter) {
            case ALL_ACTIVE -> true;
            case NEVER_OPENED -> question.getTimesShown() == 0;
            case OPENED_NOT_ANSWERED -> question.getTimesShown() > 0 && question.getTotalAttempts() == 0;
            case NOT_MASTERED -> question.getGoodCount() + question.getEasyCount() == 0;
            case AGAIN_HARD -> question.getAgainCount() + question.getHardCount() > 0;
        };
    }

    /**
     * Saves user's written answer before the reference answer is shown.
     */
    @Transactional
    public PracticeAttempt submitAnswer(Long questionId, String userAnswer) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        if (userAnswer == null || userAnswer.isBlank()) {
            throw new IllegalArgumentException("Answer cannot be empty");
        }

        PracticeAttempt attempt = new PracticeAttempt();
        attempt.setQuestion(question);
        attempt.setUserAnswer(userAnswer.trim());

        return practiceAttemptRepository.save(attempt);
    }

    /**
     * Runs AI check for a saved answer. The attempt remains usable even if AI is not configured or fails.
     */
    @Transactional
    public PracticeAttempt evaluateAnswerWithAi(Long attemptId) {
        PracticeAttempt attempt = practiceAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Practice attempt not found: " + attemptId));

        if (attempt.getAiEvaluatedAt() != null || attempt.getAiEvaluationError() != null) {
            return attempt;
        }

        try {
            AiEvaluationResult result = aiEvaluationService.evaluate(attempt);
            applyAiEvaluation(attempt, result);
        } catch (Exception e) {
            attempt.setAiEvaluationError(limitError(e.getMessage()));
        }

        return attempt;
    }

    /**
     * Registers user's self-evaluation for a saved attempt.
     */
    @Transactional
    public PracticeAttempt submitGrade(Long attemptId, PracticeGrade grade) {
        PracticeAttempt attempt = practiceAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Practice attempt not found: " + attemptId));

        if (attempt.isGraded()) {
            return attempt;
        }

        Question question = attempt.getQuestion();
        boolean correct = grade != PracticeGrade.AGAIN;

        question.registerAnswer(correct, true);
        question.registerGrade(grade);
        question.setNextReviewAt(calculateNextReviewAt(grade));

        attempt.setGrade(grade);
        attempt.setGradedAt(LocalDateTime.now());

        return attempt;
    }

    @Transactional(readOnly = true)
    public Question findQuestionForPractice(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
    }

    @Transactional(readOnly = true)
    public PracticeAttempt findAttempt(Long attemptId) {
        return practiceAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Practice attempt not found: " + attemptId));
    }

    @Transactional
    public void resetQuestionPracticeStats(Long questionId) {
        practiceAttemptRepository.deleteByQuestionId(questionId);
        questionRepository.resetPracticeStatsByQuestionId(questionId);
    }

    @Transactional
    public void resetSectionPracticeStats(Long sectionId) {
        practiceAttemptRepository.deleteBySectionId(sectionId);
        questionRepository.resetPracticeStatsBySectionId(sectionId);
    }

    @Transactional
    public void resetVolumePracticeStats(Long volumeId) {
        practiceAttemptRepository.deleteByVolumeId(volumeId);
        questionRepository.resetPracticeStatsByVolumeId(volumeId);
    }

    private LocalDateTime calculateNextReviewAt(PracticeGrade grade) {
        LocalDateTime now = LocalDateTime.now();

        return switch (grade) {
            case AGAIN -> now.plusMinutes(10);
            case HARD -> now.plusDays(1);
            case GOOD -> now.plusDays(3);
            case EASY -> now.plusDays(7);
        };
    }

    private void applyAiEvaluation(PracticeAttempt attempt, AiEvaluationResult result) {
        attempt.setAiScorePercent(result.scorePercent());
        attempt.setAiSuggestedGrade(result.suggestedGrade());
        attempt.setAiFeedback(result.feedback());
        attempt.setAiDetails(result.details());
        attempt.setAiMissingPoints(result.missingPoints());
        attempt.setAiWrongParts(result.wrongParts());
        attempt.setAiGoodParts(result.goodParts());
        attempt.setAiFollowUpSuggestion(result.followUpSuggestion());
        attempt.setAiEvaluatedAt(LocalDateTime.now());
        attempt.setAiEvaluationError(null);
    }

    private String limitError(String message) {
        if (message == null || message.isBlank()) {
            return "AI evaluation failed";
        }

        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
