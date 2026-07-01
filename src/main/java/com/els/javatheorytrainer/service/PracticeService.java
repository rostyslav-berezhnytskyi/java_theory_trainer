package com.els.javatheorytrainer.service;

import com.els.javatheorytrainer.entity.PracticeAttempt;
import com.els.javatheorytrainer.entity.Question;
import com.els.javatheorytrainer.enums.PracticeGrade;
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
        List<Question> candidates = questionRepository.findByStatusAndSectionId(
                QuestionStatus.ACTIVE,
                sectionId
        );

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No active questions in selected section");
        }

        if (excludeQuestionId != null && candidates.size() > 1) {
            candidates = candidates.stream()
                    .filter(question -> !question.getId().equals(excludeQuestionId))
                    .toList();
        }

        // Shuffle first, so questions with equal stats are not always in the same order.
        candidates = new java.util.ArrayList<>(candidates);
        Collections.shuffle(candidates);

        candidates.sort(
                Comparator.comparingInt(Question::getTimesShown)
                        .thenComparing(Comparator.comparingInt(Question::getWrongTotalCount).reversed())
                        .thenComparing(Question::getId)
        );

        Question question = candidates.getFirst();
        question.markAsShown();

        return question;
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

    private LocalDateTime calculateNextReviewAt(PracticeGrade grade) {
        LocalDateTime now = LocalDateTime.now();

        return switch (grade) {
            case AGAIN -> now.plusMinutes(10);
            case HARD -> now.plusDays(1);
            case GOOD -> now.plusDays(3);
            case EASY -> now.plusDays(7);
        };
    }
}
