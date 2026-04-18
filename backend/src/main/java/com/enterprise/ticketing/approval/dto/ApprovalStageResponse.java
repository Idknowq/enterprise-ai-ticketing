package com.enterprise.ticketing.approval.dto;

import com.enterprise.ticketing.approval.domain.ApprovalStatus;
import java.time.Instant;

public record ApprovalStageResponse(
        Long id,
        String workflowId,
        String aiWorkflowId,
        int stageOrder,
        String stageKey,
        String stageDisplayName,
        ApprovalStatus status,
        Long approverId,
        String approverName,
        String approverUsername,
        String comment,
        Instant requestedAt,
        Instant decidedAt
) {
}
