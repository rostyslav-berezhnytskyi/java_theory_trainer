package com.els.javatheorytrainer.service;

import com.els.javatheorytrainer.entity.Question;
import com.els.javatheorytrainer.enums.PracticeGrade;
import com.els.javatheorytrainer.enums.QuestionStatus;
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
     * Registers user's self-evaluation.
     */
    @Transactional
    public Question submitGrade(Long questionId, PracticeGrade grade) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        boolean correct = grade != PracticeGrade.AGAIN;

        // In this first version each question has only one visible attempt.
        question.registerAnswer(correct, true);
        question.setNextReviewAt(calculateNextReviewAt(grade));

        return question;
    }

    @Transactional(readOnly = true)
    public Question findQuestionForPractice(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
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
