package com.enterprise.ticketing.approval.repository;

import com.enterprise.ticketing.approval.domain.ApprovalStatus;
import com.enterprise.ticketing.approval.entity.ApprovalEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApprovalRepository extends JpaRepository<ApprovalEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"ticket", "ticket.requester", "ticket.assignee", "approver", "approver.roles"})
    Optional<ApprovalEntity> findById(Long id);

    @EntityGraph(attributePaths = {"ticket", "ticket.requester", "ticket.assignee", "approver", "approver.roles"})
    List<ApprovalEntity> findByTicketIdOrderByStageOrderAsc(Long ticketId);

    @EntityGraph(attributePaths = {"ticket", "ticket.requester", "ticket.assignee", "approver", "approver.roles"})
    List<ApprovalEntity> findByWorkflowIdOrderByStageOrderAsc(String workflowId);

    @EntityGraph(attributePaths = {"ticket", "ticket.requester", "ticket.assignee", "approver", "approver.roles"})
    Optional<ApprovalEntity> findByWorkflowIdAndStageOrder(String workflowId, int stageOrder);

    @EntityGraph(attributePaths = {"ticket", "ticket.requester", "ticket.assignee", "approver", "approver.roles"})
    Optional<ApprovalEntity> findFirstByTicketIdAndStatusOrderByStageOrderAsc(Long ticketId, ApprovalStatus status);

    @EntityGraph(attributePaths = {"ticket", "ticket.requester", "ticket.assignee", "approver", "approver.roles"})
    @Query("""
            select a
            from ApprovalEntity a
            where a.status = :status
              and (:approverId is null or a.approver.id = :approverId)
            order by a.requestedAt asc, a.id asc
            """)
    List<ApprovalEntity> findPendingApprovals(ApprovalStatus status, Long approverId);

    boolean existsByWorkflowId(String workflowId);

    long countByStatus(ApprovalStatus status);

    @Query(
            value = """
                    select avg(extract(epoch from (decided_at - requested_at)) * 1000)
                    from approvals
                    where status <> 'PENDING' and decided_at is not null
                    """,
            nativeQuery = true
    )
    Double averageDecisionLatencyMs();
}
