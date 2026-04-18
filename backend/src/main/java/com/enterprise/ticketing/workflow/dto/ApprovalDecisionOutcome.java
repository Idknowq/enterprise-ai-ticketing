package com.enterprise.ticketing.workflow.dto;

import com.enterprise.ticketing.approval.domain.ApprovalStatus;

public record ApprovalDecisionOutcome(
        Long approvalId,
        ApprovalStatus status,
        boolean idempotent,
        boolean terminal,
        long waitDurationMs
) {
}
