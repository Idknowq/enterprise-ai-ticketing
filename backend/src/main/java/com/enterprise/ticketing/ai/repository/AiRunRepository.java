package com.enterprise.ticketing.ai.repository;

import com.enterprise.ticketing.ai.entity.AiRunEntity;
import com.enterprise.ticketing.ai.domain.AiNodeName;
import com.enterprise.ticketing.ai.domain.AiRunStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AiRunRepository extends JpaRepository<AiRunEntity, Long> {

    List<AiRunEntity> findByTicketIdOrderByCreatedAtDescIdDesc(Long ticketId);

    long countByNodeName(AiNodeName nodeName);

    long countByNodeNameAndStatus(AiNodeName nodeName, AiRunStatus status);

    @Query("""
            select avg(a.latencyMs)
            from AiRunEntity a
            where a.nodeName = :nodeName and (:status is null or a.status = :status)
            """)
    Double averageLatencyByNodeAndStatus(AiNodeName nodeName, AiRunStatus status);
}
