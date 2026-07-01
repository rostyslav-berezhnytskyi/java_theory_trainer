package com.els.javatheorytrainer.repository;

import com.els.javatheorytrainer.entity.Volume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VolumeRepository extends JpaRepository<Volume, Long> {

    List<Volume> findAllByOrderBySortOrderAscTitleAsc();

    @Query("select coalesce(max(v.sortOrder), 0) from Volume v")
    int findMaxSortOrder();
}
