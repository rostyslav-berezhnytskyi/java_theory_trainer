package com.els.javatheorytrainer.entity;

import com.els.javatheorytrainer.enums.ImageRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Image attached to a question.
 *
 * One question can have many images.
 * ImageRole defines whether the image belongs to the question or to the answer.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "question_images")
public class QuestionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent question.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /**
     * Defines where this image should be displayed.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImageRole role = ImageRole.ANSWER;

    /**
     * Image URL or local path.
     *
     * Example:
     * /uploads/questions/2026/06/jvm-diagram.webp
     */
    @Column(nullable = false, length = 500)
    private String imageUrl;

    /**
     * Alternative text for accessibility.
     */
    @Column(length = 300)
    private String altText;

    /**
     * Optional text shown under image.
     */
    @Column(columnDefinition = "text")
    private String caption;

    /**
     * Manual order of images.
     */
    @Column(nullable = false)
    private int sortOrder = 0;
}
