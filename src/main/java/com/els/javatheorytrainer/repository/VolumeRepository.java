package com.els.javatheorytrainer.repository;

import com.els.javatheorytrainer.entity.Volume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VolumeRepository extends JpaRepository<Volume, Long> {

    List<Volume> findAllByOrderBySortOrderAscTitleAsc();

    @Query("""
            select v
            from Volume v
            where (:search is null
                or lower(v.title) like concat('%', cast(:search as string), '%')
                or lower(v.slug) like concat('%', cast(:search as string), '%'))
              and (:active is null or v.active = :active)
            order by v.sortOrder asc, v.title asc
            """)
    Page<Volume> findAdminPage(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("select coalesce(max(v.sortOrder), 0) from Volume v")
    int findMaxSortOrder();
}
