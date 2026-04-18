package com.enterprise.ticketing.approval.dto;

import com.enterprise.ticketing.approval.domain.ApprovalStatus;
import com.enterprise.ticketing.ticket.domain.TicketStatus;
import java.time.Instant;

public record PendingApprovalResponse(
        Long approvalId,
        Long ticketId,
        String ticketTitle,
        TicketStatus ticketStatus,
        String workflowId,
        String aiWorkflowId,
        int stageOrder,
        String stageKey,
        String stageDisplayName,
        ApprovalStatus approvalStatus,
        Long approverId,
        String approverName,
        Instant requestedAt
) {
}
