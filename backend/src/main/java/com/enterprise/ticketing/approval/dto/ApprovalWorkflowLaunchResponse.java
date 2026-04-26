package com.enterprise.ticketing.approval.dto;

public record ApprovalWorkflowLaunchResponse(
        Long ticketId,
        String workflowId,
        boolean started,
        boolean alreadyWaitingApproval,
        boolean manualReviewRequired,
        String message
) {
}
