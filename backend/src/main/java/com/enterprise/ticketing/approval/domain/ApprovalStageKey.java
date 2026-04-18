package com.enterprise.ticketing.approval.domain;

import com.enterprise.ticketing.auth.domain.SystemRole;

public enum ApprovalStageKey {
    LINE_MANAGER(1, "Line manager approval", SystemRole.APPROVER),
    SYSTEM_ADMIN(2, "System administrator approval", SystemRole.ADMIN);

    private final int order;
    private final String displayName;
    private final SystemRole resolverRole;

    ApprovalStageKey(int order, String displayName, SystemRole resolverRole) {
        this.order = order;
        this.displayName = displayName;
        this.resolverRole = resolverRole;
    }

    public int getOrder() {
        return order;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SystemRole getResolverRole() {
        return resolverRole;
    }
}
