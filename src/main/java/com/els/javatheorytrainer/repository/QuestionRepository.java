package com.els.javatheorytrainer.repository;

import com.els.javatheorytrainer.dto.PracticeProgressStats;
import com.els.javatheorytrainer.entity.Question;
import com.els.javatheorytrainer.enums.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findAllByOrderBySectionVolumeSortOrderAscSectionSortOrderAscSortOrderAscIdAsc();

    @Query(
            value = """
                    select q
                    from Question q
                    join fetch q.section s
                    join fetch s.volume v
                    where (:search is null
                        or lower(q.questionText) like concat('%', cast(:search as string), '%')
                        or lower(s.title) like concat('%', cast(:search as string), '%')
                        or lower(v.title) like concat('%', cast(:search as string), '%'))
                      and (:volumeId is null or v.id = :volumeId)
                      and (:sectionId is null or s.id = :sectionId)
                      and (:tag is null or lower(q.tags) like concat('%', cast(:tag as string), '%'))
                      and (:difficulty is null or q.difficulty = :difficulty)
                      and (:status is null or q.status = :status)
                    order by v.sortOrder asc, s.sortOrder asc, q.sortOrder asc, q.id asc
                    """,
            countQuery = """
                    select count(q)
                    from Question q
                    join q.section s
                    join s.volume v
                    where (:search is null
                        or lower(q.questionText) like concat('%', cast(:search as string), '%')
                        or lower(s.title) like concat('%', cast(:search as string), '%')
                        or lower(v.title) like concat('%', cast(:search as string), '%'))
                      and (:volumeId is null or v.id = :volumeId)
                      and (:sectionId is null or s.id = :sectionId)
                      and (:tag is null or lower(q.tags) like concat('%', cast(:tag as string), '%'))
                      and (:difficulty is null or q.difficulty = :difficulty)
                      and (:status is null or q.status = :status)
                    """
    )
    Page<Question> findAdminPage(
            @Param("search") String search,
            @Param("volumeId") Long volumeId,
            @Param("sectionId") Long sectionId,
            @Param("tag") String tag,
            @Param("difficulty") com.els.javatheorytrainer.enums.Difficulty difficulty,
            @Param("status") QuestionStatus status,
            Pageable pageable
    );

    List<Question> findBySectionIdOrderBySortOrderAscIdAsc(Long sectionId);

    List<Question> findByStatusOrderBySectionSortOrderAscSortOrderAscIdAsc(QuestionStatus status);

    List<Question> findByStatusAndSectionId(QuestionStatus status, Long sectionId);

    List<Question> findByStatusAndSectionVolumeId(QuestionStatus status, Long volumeId);

    @Query("select coalesce(max(q.sortOrder), 0) from Question q where q.section.id = :sectionId")
    int findMaxSortOrderBySectionId(@Param("sectionId") Long sectionId);

    @Modifying
    @Query("""
            update Question q
            set q.timesShown = 0,
                q.totalAttempts = 0,
                q.correctFirstTryCount = 0,
                q.correctTotalCount = 0,
                q.wrongTotalCount = 0,
                q.againCount = 0,
                q.hardCount = 0,
                q.goodCount = 0,
                q.easyCount = 0,
                q.lastShownAt = null,
                q.lastAnsweredAt = null,
                q.nextReviewAt = null
            where q.id = :questionId
            """)
    void resetPracticeStatsByQuestionId(@Param("questionId") Long questionId);

    @Modifying
    @Query("""
            update Question q
            set q.timesShown = 0,
                q.totalAttempts = 0,
                q.correctFirstTryCount = 0,
                q.correctTotalCount = 0,
                q.wrongTotalCount = 0,
                q.againCount = 0,
                q.hardCount = 0,
                q.goodCount = 0,
                q.easyCount = 0,
                q.lastShownAt = null,
                q.lastAnsweredAt = null,
                q.nextReviewAt = null
            where q.section.id = :sectionId
            """)
    void resetPracticeStatsBySectionId(@Param("sectionId") Long sectionId);

    @Modifying
    @Query("""
            update Question q
            set q.timesShown = 0,
                q.totalAttempts = 0,
                q.correctFirstTryCount = 0,
                q.correctTotalCount = 0,
                q.wrongTotalCount = 0,
                q.againCount = 0,
                q.hardCount = 0,
                q.goodCount = 0,
                q.easyCount = 0,
                q.lastShownAt = null,
                q.lastAnsweredAt = null,
                q.nextReviewAt = null
            where q.section.volume.id = :volumeId
            """)
    void resetPracticeStatsByVolumeId(@Param("volumeId") Long volumeId);

    @Query("""
            select new com.els.javatheorytrainer.dto.PracticeProgressStats(
                q.section.id,
                count(q),
                sum(case when q.timesShown > 0 then 1 else 0 end),
                sum(case when q.totalAttempts > 0 then 1 else 0 end),
                sum(case when (q.goodCount + q.easyCount) > 0 then 1 else 0 end),
                sum(q.totalAttempts),
                sum(q.againCount),
                sum(q.hardCount),
                sum(q.goodCount),
                sum(q.easyCount)
            )
            from Question q
            where q.status = com.els.javatheorytrainer.enums.QuestionStatus.ACTIVE
            group by q.section.id
            """)
    List<PracticeProgressStats> findSectionProgressStats();

    @Query("""
            select new com.els.javatheorytrainer.dto.PracticeProgressStats(
                q.section.volume.id,
                count(q),
                sum(case when q.timesShown > 0 then 1 else 0 end),
                sum(case when q.totalAttempts > 0 then 1 else 0 end),
                sum(case when (q.goodCount + q.easyCount) > 0 then 1 else 0 end),
                sum(q.totalAttempts),
                sum(q.againCount),
                sum(q.hardCount),
                sum(q.goodCount),
                sum(q.easyCount)
            )
            from Question q
            where q.status = com.els.javatheorytrainer.enums.QuestionStatus.ACTIVE
            group by q.section.volume.id
            """)
    List<PracticeProgressStats> findVolumeProgressStats();
}
