package com.enterprise.ticketing.auth.domain;

public enum SystemRole {
    EMPLOYEE,
    SUPPORT_AGENT,
    APPROVER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
