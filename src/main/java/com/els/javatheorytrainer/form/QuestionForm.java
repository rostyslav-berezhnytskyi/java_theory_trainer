package com.els.javatheorytrainer.form;

import com.els.javatheorytrainer.enums.Difficulty;
import com.els.javatheorytrainer.enums.QuestionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Form object for creating and editing questions.
 */
@Getter
@Setter
@NoArgsConstructor
public class QuestionForm {

    private Long id;

    /**
     * Selected volume.
     * Used only by UI to filter sections.
     */
    private Long volumeId;

    /**
     * Selected section.
     * This is the real relation used for Question entity.
     */
    private Long sectionId;

    private String questionText;

    private String shortAnswer;

    private String fullAnswer;

    private String theoryNotes;

    private String hint;

    private String mustHavePointsText;

    private String commonMistakesText;

    private String tags;

    private String sourceReference;

    private Difficulty difficulty = Difficulty.MEDIUM;

    private QuestionStatus status = QuestionStatus.DRAFT;

    private int sortOrder = 0;
}