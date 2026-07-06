package com.els.javatheorytrainer.repository;

import com.els.javatheorytrainer.entity.Section;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findAllByOrderByVolumeSortOrderAscSortOrderAscTitleAsc();

    @Query(
            value = """
                    select s
                    from Section s
                    join fetch s.volume v
                    where (:search is null
                        or lower(s.title) like concat('%', cast(:search as string), '%')
                        or lower(s.slug) like concat('%', cast(:search as string), '%')
                        or lower(v.title) like concat('%', cast(:search as string), '%'))
                      and (:volumeId is null or v.id = :volumeId)
                      and (:active is null or s.active = :active)
                    order by v.sortOrder asc, s.sortOrder asc, s.title asc
                    """,
            countQuery = """
                    select count(s)
                    from Section s
                    join s.volume v
                    where (:search is null
                        or lower(s.title) like concat('%', cast(:search as string), '%')
                        or lower(s.slug) like concat('%', cast(:search as string), '%')
                        or lower(v.title) like concat('%', cast(:search as string), '%'))
                      and (:volumeId is null or v.id = :volumeId)
                      and (:active is null or s.active = :active)
                    """
    )
    Page<Section> findAdminPage(
            @Param("search") String search,
            @Param("volumeId") Long volumeId,
            @Param("active") Boolean active,
            Pageable pageable
    );

    List<Section> findByVolumeIdOrderBySortOrderAscTitleAsc(Long volumeId);

    @Query("select coalesce(max(s.sortOrder), 0) from Section s where s.volume.id = :volumeId")
    int findMaxSortOrderByVolumeId(@Param("volumeId") Long volumeId);
}
