package com.enterprise.ticketing.approval.dto;

import com.enterprise.ticketing.approval.domain.ApprovalStatus;
import java.time.Instant;

public record ApprovalDecisionResponse(
        Long id,
        Long ticketId,
        String workflowId,
        int stageOrder,
        String stageKey,
        ApprovalStatus status,
        String comment,
        boolean idempotent,
        Instant requestedAt,
        Instant decidedAt
) {
}
