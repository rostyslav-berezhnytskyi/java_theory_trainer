package com.els.javatheorytrainer.repository;

import com.els.javatheorytrainer.entity.Question;
import com.els.javatheorytrainer.enums.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findAllByOrderBySectionVolumeSortOrderAscSectionSortOrderAscSortOrderAscIdAsc();

    List<Question> findBySectionIdOrderBySortOrderAscIdAsc(Long sectionId);

    List<Question> findByStatusOrderBySectionSortOrderAscSortOrderAscIdAsc(QuestionStatus status);

    List<Question> findByStatusAndSectionId(QuestionStatus status, Long sectionId);
}
