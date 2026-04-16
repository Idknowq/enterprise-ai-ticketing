package com.enterprise.ticketing.ticket.domain;

import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import java.util.EnumSet;

public enum TicketStatus {
    OPEN,
    AI_PROCESSING,
    WAITING_APPROVAL,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    REJECTED;

    public boolean isTerminal() {
        return this == CLOSED || this == REJECTED;
    }

    public boolean canTransitionTo(TicketStatus targetStatus) {
        if (targetStatus == null || this == targetStatus) {
            return false;
        }
        return switch (this) {
            case OPEN -> EnumSet.of(AI_PROCESSING, WAITING_APPROVAL, IN_PROGRESS, RESOLVED, REJECTED, CLOSED)
                    .contains(targetStatus);
            case AI_PROCESSING -> EnumSet.of(WAITING_APPROVAL, IN_PROGRESS, RESOLVED, REJECTED, OPEN)
                    .contains(targetStatus);
            case WAITING_APPROVAL -> EnumSet.of(IN_PROGRESS, REJECTED).contains(targetStatus);
            case IN_PROGRESS -> EnumSet.of(WAITING_APPROVAL, RESOLVED, REJECTED, CLOSED).contains(targetStatus);
            case RESOLVED -> EnumSet.of(IN_PROGRESS, CLOSED).contains(targetStatus);
            case REJECTED -> EnumSet.of(CLOSED).contains(targetStatus);
            case CLOSED -> false;
        };
    }

    public void validateTransitionTo(TicketStatus targetStatus) {
        if (!canTransitionTo(targetStatus)) {
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT,
                    "Invalid ticket status transition: " + this + " -> " + targetStatus
            );
        }
    }
}
