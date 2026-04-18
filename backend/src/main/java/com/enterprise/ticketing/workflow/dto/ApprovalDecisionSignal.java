package com.enterprise.ticketing.workflow.dto;

import com.enterprise.ticketing.approval.domain.ApprovalAction;

public record ApprovalDecisionSignal(
        Long approvalId,
        ApprovalAction action,
        Long operatorId,
        String operatorName,
        String comment,
        String requestId,
        String traceId
) {
}
