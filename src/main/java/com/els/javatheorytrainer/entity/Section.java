package com.els.javatheorytrainer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Section inside a volume.
 *
 * Example:
 * Volume  = Java Core
 * Section = JVM, Memory
 *
 * This is an entity, not enum, because you should be able to add new sections from admin UI.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "sections",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sections_volume_slug", columnNames = {"volume_id", "slug"})
        }
)
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent volume.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "volume_id", nullable = false)
    private Volume volume;

    /**
     * Human-readable section title.
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Slug is unique only inside one volume.
     *
     * Example:
     * Java Core / JVM, Memory -> jvm-memory
     */
    @Column(nullable = false, length = 200)
    private String slug;

    /**
     * Optional section description.
     */
    @Column(columnDefinition = "text")
    private String description;

    /**
     * Manual order inside volume.
     */
    @Column(nullable = false)
    private int sortOrder = 0;

    /**
     * Simple soft-hide for section.
     */
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
