package com.els.javatheorytrainer.form;

import com.els.javatheorytrainer.enums.Difficulty;
import com.els.javatheorytrainer.enums.QuestionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Form object for creating and editing questions.
 *
 * We use this class instead of binding HTML form directly to Question entity.
 * It keeps form processing simple, especially for multiline fields like
 * must-have points and common mistakes.
 */
@Getter
@Setter
@NoArgsConstructor
public class QuestionForm {

    private Long id;

    private Long sectionId;

    private String questionText;

    private String shortAnswer;

    private String fullAnswer;

    private String hint;

    /**
     * Multiline text.
     * Each line will become one item in Question.mustHavePoints.
     */
    private String mustHavePointsText;

    /**
     * Multiline text.
     * Each line will become one item in Question.commonMistakes.
     */
    private String commonMistakesText;

    private String tags;

    private String sourceReference;

    private Difficulty difficulty = Difficulty.MEDIUM;

    private QuestionStatus status = QuestionStatus.DRAFT;

    private int sortOrder = 0;
}
