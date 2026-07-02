package com.els.javatheorytrainer.repository;

import com.els.javatheorytrainer.entity.PracticeAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PracticeAttemptRepository extends JpaRepository<PracticeAttempt, Long> {

    @Modifying
    @Query("delete from PracticeAttempt a where a.question.id = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);

    @Modifying
    @Query("delete from PracticeAttempt a where a.question.section.id = :sectionId")
    void deleteBySectionId(@Param("sectionId") Long sectionId);

    @Modifying
    @Query("delete from PracticeAttempt a where a.question.section.volume.id = :volumeId")
    void deleteByVolumeId(@Param("volumeId") Long volumeId);
}
