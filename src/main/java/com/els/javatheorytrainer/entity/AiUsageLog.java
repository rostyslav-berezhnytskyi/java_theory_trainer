package com.els.javatheorytrainer.entity;

import com.els.javatheorytrainer.enums.AiUsageOperation;
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
@Table(name = "ai_usage_logs")
public class AiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AiUsageOperation operation;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false)
    private int inputChars;

    @Column(nullable = false)
    private int outputChars;

    private Long audioBytes;

    @Column(nullable = false)
    private boolean success;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_attempt_id")
    private PracticeAttempt practiceAttempt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
