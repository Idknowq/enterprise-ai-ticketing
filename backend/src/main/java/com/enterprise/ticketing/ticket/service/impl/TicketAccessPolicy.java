package com.enterprise.ticketing.ticket.service.impl;

import com.enterprise.ticketing.auth.access.AccessControlService;
import com.enterprise.ticketing.auth.domain.SystemRole;
import com.enterprise.ticketing.auth.security.UserPrincipal;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.ticket.domain.TicketStatus;
import com.enterprise.ticketing.ticket.entity.TicketEntity;
import org.springframework.stereotype.Component;

@Component
public class TicketAccessPolicy {

    private final AccessControlService accessControlService;

    public TicketAccessPolicy(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    public UserPrincipal currentUser() {
        return accessControlService.currentUser();
    }

    public Long currentUserId() {
        return currentUser().getId();
    }

    public void checkReadAccess(TicketEntity ticket) {
        if (!canRead(ticket)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "Current user cannot access this ticket");
        }
    }

    public void checkCommentAccess(TicketEntity ticket) {
        checkReadAccess(ticket);
    }

    public void checkAssignmentAccess() {
        accessControlService.checkAnyRole(SystemRole.SUPPORT_AGENT, SystemRole.ADMIN);
    }

    public void checkManualStatusUpdateAccess(TicketEntity ticket, TicketStatus targetStatus) {
        if (canManageAllTickets()) {
            return;
        }
        if (isRequester(ticket) && ticket.getStatus() == TicketStatus.RESOLVED && targetStatus == TicketStatus.CLOSED) {
            return;
        }
        throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "Current user cannot update ticket status");
    }

    public boolean canManageAllTickets() {
        return accessControlService.hasAnyRole(SystemRole.SUPPORT_AGENT, SystemRole.ADMIN);
    }

    public boolean isApprover() {
        return accessControlService.hasRole(SystemRole.APPROVER);
    }

    private boolean canRead(TicketEntity ticket) {
        if (canManageAllTickets()) {
            return true;
        }
        if (isRequester(ticket) || isAssignee(ticket)) {
            return true;
        }
        return isApprover() && ticket.getStatus() == TicketStatus.WAITING_APPROVAL;
    }

    private boolean isRequester(TicketEntity ticket) {
        return ticket.getRequester() != null && accessControlService.isCurrentUser(ticket.getRequester().getId());
    }

    private boolean isAssignee(TicketEntity ticket) {
        return ticket.getAssignee() != null && accessControlService.isCurrentUser(ticket.getAssignee().getId());
    }
}
