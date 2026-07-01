package com.els.javatheorytrainer.entity;

import com.els.javatheorytrainer.enums.Difficulty;
import com.els.javatheorytrainer.enums.QuestionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Main entity of the application.
 *
 * One Question contains:
 * - question text;
 * - short reference answer;
 * - full reference answer;
 * - optional hint;
 * - AI evaluation helper data;
 * - practice statistics;
 * - related images.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Section where this question belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    /**
     * Main question text.
     */
    @Column(nullable = false, columnDefinition = "text")
    private String questionText;

    /**
     * Short answer for quick interview-style revision.
     */
    @Column(columnDefinition = "text")
    private String shortAnswer;

    /**
     * Full detailed answer.
     * Later we can render it as Markdown in Thymeleaf.
     */
    @Column(columnDefinition = "text")
    private String fullAnswer;

    /**
     * Optional deep theory notes.
     *
     * This field is for long explanations from your notes.
     * It is not required for every question.
     */
    @Column(columnDefinition = "text")
    private String theoryNotes;

    /**
     * Optional hint shown when you do not know the answer.
     */
    @Column(columnDefinition = "text")
    private String hint;

    /**
     * Key points that must be present in a good answer.
     *
     * Stored as separate rows in question_must_have_points table.
     * This is better than one big String because AI/checking logic can work with a real list.
     */
    @ElementCollection
    @CollectionTable(
            name = "question_must_have_points",
            joinColumns = @JoinColumn(name = "question_id")
    )
    @Column(name = "point", nullable = false, columnDefinition = "text")
    @OrderColumn(name = "position")
    private List<String> mustHavePoints = new ArrayList<>();

    /**
     * Common mistakes or wrong explanations for this question.
     */
    @ElementCollection
    @CollectionTable(
            name = "question_common_mistakes",
            joinColumns = @JoinColumn(name = "question_id")
    )
    @Column(name = "mistake", nullable = false, columnDefinition = "text")
    @OrderColumn(name = "position")
    private List<String> commonMistakes = new ArrayList<>();

    /**
     * Simple comma-separated tags.
     *
     * Example:
     * jvm,memory,interview-basic
     *
     * Tags are not strict business data, so String is acceptable for MVP.
     * Later we can move tags to a separate entity if needed.
     */
    @Column(length = 500)
    private String tags;

    /**
     * Optional reference to original notes.
     *
     * Example:
     * Java Core.docx, JVM Memory, page 5
     */
    @Column(length = 300)
    private String sourceReference;

    /**
     * Difficulty is enum because the list is small and stable.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Difficulty difficulty = Difficulty.MEDIUM;

    /**
     * Status replaces boolean "active".
     *
     * ACTIVE   - visible in practice
     * DRAFT    - not ready yet
     * ARCHIVED - soft-deleted/hidden
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionStatus status = QuestionStatus.DRAFT;

    /**
     * Manual order inside section.
     */
    @Column(nullable = false)
    private int sortOrder = 0;

    /**
     * Images attached to this question.
     *
     * Can contain multiple QUESTION images and multiple ANSWER images.
     */
    @OneToMany(
            mappedBy = "question",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sortOrder ASC")
    private List<QuestionImage> images = new ArrayList<>();

    /**
     * How many times this question was shown in practice mode.
     */
    @Column(nullable = false)
    private int timesShown = 0;

    /**
     * How many total attempts were made.
     */
    @Column(nullable = false)
    private int totalAttempts = 0;

    /**
     * How many times the first answer attempt was correct.
     */
    @Column(nullable = false)
    private int correctFirstTryCount = 0;

    /**
     * How many correct answers were given in total.
     */
    @Column(nullable = false)
    private int correctTotalCount = 0;

    /**
     * How many wrong answers were given in total.
     */
    @Column(nullable = false)
    private int wrongTotalCount = 0;

    private LocalDateTime lastShownAt;

    private LocalDateTime lastAnsweredAt;

    /**
     * Used later for spaced repetition.
     */
    private LocalDateTime nextReviewAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Optimistic locking.
     * Protects from accidental overwrites.
     */
    @Version
    private Long version;

    /**
     * Helper method for adding image correctly.
     */
    public void addImage(QuestionImage image) {
        images.add(image);
        image.setQuestion(this);
    }

    /**
     * Helper method for removing image correctly.
     */
    public void removeImage(QuestionImage image) {
        images.remove(image);
        image.setQuestion(null);
    }

    /**
     * Returns true only if question is ready for practice.
     */
    public boolean isActive() {
        return status == QuestionStatus.ACTIVE;
    }

    /**
     * Call this when question is shown to the user.
     */
    public void markAsShown() {
        this.timesShown++;
        this.lastShownAt = LocalDateTime.now();
    }

    /**
     * Call this when user answers the question.
     *
     * @param correct  true if answer is accepted as correct
     * @param firstTry true if this was the first attempt in the current practice session
     */
    public void registerAnswer(boolean correct, boolean firstTry) {
        this.totalAttempts++;
        this.lastAnsweredAt = LocalDateTime.now();

        if (correct) {
            this.correctTotalCount++;

            if (firstTry) {
                this.correctFirstTryCount++;
            }
        } else {
            this.wrongTotalCount++;
        }
    }
}
