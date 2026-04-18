package com.enterprise.ticketing.workflow.dto;

import com.enterprise.ticketing.approval.domain.ApprovalStageKey;

public record ApprovalStageExecution(
        Long approvalId,
        ApprovalStageKey stageKey,
        int stageOrder,
        Long approverId,
        String approverName
) {
}
