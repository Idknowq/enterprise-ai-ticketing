package com.enterprise.ticketing.workflow.dto;

public record ApprovalWorkflowInput(
        Long ticketId,
        String workflowId,
        String aiWorkflowId,
        String ticketCategory,
        String traceId
) {
}
