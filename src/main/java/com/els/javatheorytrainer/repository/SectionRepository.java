package com.els.javatheorytrainer.repository;

import com.els.javatheorytrainer.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findAllByOrderByVolumeSortOrderAscSortOrderAscTitleAsc();

    List<Section> findByVolumeIdOrderBySortOrderAscTitleAsc(Long volumeId);

    @Query("select coalesce(max(s.sortOrder), 0) from Section s where s.volume.id = :volumeId")
    int findMaxSortOrderByVolumeId(@Param("volumeId") Long volumeId);
}
