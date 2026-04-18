package com.enterprise.ticketing.ticket.domain;

public enum TicketEventType {
    CREATED,
    STATUS_CHANGED,
    COMMENT_ADDED,
    ASSIGNED,
    WORKFLOW_STARTED,
    WORKFLOW_RESUMED,
    WORKFLOW_COMPLETED,
    APPROVAL_REQUESTED,
    APPROVAL_APPROVED,
    APPROVAL_REJECTED
}
