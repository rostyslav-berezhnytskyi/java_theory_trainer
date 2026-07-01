package com.els.javatheorytrainer.entity;

import com.els.javatheorytrainer.enums.PracticeGrade;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "practice_attempts")
public class PracticeAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, columnDefinition = "text")
    private String userAnswer;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PracticeGrade grade;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime gradedAt;

    public boolean isGraded() {
        return grade != null;
    }
}
