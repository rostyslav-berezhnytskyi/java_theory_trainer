package com.els.javatheorytrainer.repository;

import com.els.javatheorytrainer.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findAllByOrderByVolumeSortOrderAscSortOrderAscTitleAsc();

    List<Section> findByVolumeIdOrderBySortOrderAscTitleAsc(Long volumeId);
}
