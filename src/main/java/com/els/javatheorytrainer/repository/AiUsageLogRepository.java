package com.els.javatheorytrainer.repository;

import com.els.javatheorytrainer.dto.AiUsageSummary;
import com.els.javatheorytrainer.entity.AiUsageLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    @Query("""
            select new com.els.javatheorytrainer.dto.AiUsageSummary(
                l.operation,
                l.model,
                count(l),
                sum(case when l.success = true then 1 else 0 end),
                sum(case when l.success = false then 1 else 0 end),
                coalesce(sum(l.inputChars), 0),
                coalesce(sum(l.outputChars), 0),
                coalesce(sum(l.audioBytes), 0)
            )
            from AiUsageLog l
            group by l.operation, l.model
            order by max(l.createdAt) desc
            """)
    List<AiUsageSummary> summarizeByOperationAndModel();

    List<AiUsageLog> findByOrderByCreatedAtDesc(Pageable pageable);
}
