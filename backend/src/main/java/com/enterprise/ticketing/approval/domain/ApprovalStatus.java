package com.enterprise.ticketing.approval.domain;

public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public boolean isFinalStatus() {
        return this == APPROVED || this == REJECTED;
    }
}
