package com.els.javatheorytrainer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Big study volume.
 *
 * Examples:
 * - Java Core
 * - Spring Web
 * - Hibernate
 * - Algorithms
 *
 * This is an entity, not enum, because you should be able to create new volumes from admin UI.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "volumes")
public class Volume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable volume title.
     */
    @Column(nullable = false, unique = true, length = 150)
    private String title;

    /**
     * URL-friendly or code-friendly name.
     *
     * Example:
     * title = "Java Core"
     * slug  = "java-core"
     */
    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    /**
     * Optional volume description.
     */
    @Column(columnDefinition = "text")
    private String description;

    /**
     * Manual order in UI.
     */
    @Column(nullable = false)
    private int sortOrder = 0;

    /**
     * Simple soft-hide for volume.
     * If false, volume can be hidden from UI without deleting it.
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
