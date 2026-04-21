package com.enterprise.ticketing.ticket.service.impl;

import com.enterprise.ticketing.auth.access.AccessControlService;
import com.enterprise.ticketing.auth.entity.UserEntity;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.ticket.domain.TicketStatus;
import com.enterprise.ticketing.ticket.entity.TicketEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketAccessPolicyTest {

    @Mock
    private AccessControlService accessControlService;

    @Test
    void checkReadAccessAllowsApproverToViewWaitingApprovalTicket() {
        TicketAccessPolicy policy = new TicketAccessPolicy(accessControlService);
        TicketEntity ticket = ticket(10L, 20L, TicketStatus.WAITING_APPROVAL);

        when(accessControlService.hasAnyRole(
                com.enterprise.ticketing.auth.domain.SystemRole.SUPPORT_AGENT,
                com.enterprise.ticketing.auth.domain.SystemRole.ADMIN
        )).thenReturn(false);
        when(accessControlService.isCurrentUser(10L)).thenReturn(false);
        when(accessControlService.isCurrentUser(20L)).thenReturn(false);
        when(accessControlService.hasRole(com.enterprise.ticketing.auth.domain.SystemRole.APPROVER)).thenReturn(true);

        assertThatCode(() -> policy.checkReadAccess(ticket)).doesNotThrowAnyException();
    }

    @Test
    void checkReadAccessRejectsUserWithoutOwnershipOrRole() {
        TicketAccessPolicy policy = new TicketAccessPolicy(accessControlService);
        TicketEntity ticket = ticket(10L, 20L, TicketStatus.IN_PROGRESS);

        when(accessControlService.hasAnyRole(
                com.enterprise.ticketing.auth.domain.SystemRole.SUPPORT_AGENT,
                com.enterprise.ticketing.auth.domain.SystemRole.ADMIN
        )).thenReturn(false);
        when(accessControlService.isCurrentUser(10L)).thenReturn(false);
        when(accessControlService.isCurrentUser(20L)).thenReturn(false);
        when(accessControlService.hasRole(com.enterprise.ticketing.auth.domain.SystemRole.APPROVER)).thenReturn(false);

        assertThatThrownBy(() -> policy.checkReadAccess(ticket))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_FORBIDDEN);
    }

    @Test
    void checkManualStatusUpdateAccessAllowsRequesterToCloseResolvedTicket() {
        TicketAccessPolicy policy = new TicketAccessPolicy(accessControlService);
        TicketEntity ticket = ticket(10L, null, TicketStatus.RESOLVED);

        when(accessControlService.hasAnyRole(
                com.enterprise.ticketing.auth.domain.SystemRole.SUPPORT_AGENT,
                com.enterprise.ticketing.auth.domain.SystemRole.ADMIN
        )).thenReturn(false);
        when(accessControlService.isCurrentUser(10L)).thenReturn(true);

        assertThatCode(() -> policy.checkManualStatusUpdateAccess(ticket, TicketStatus.CLOSED))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAssignmentAccessRequiresSupportAgentOrAdminRole() {
        TicketAccessPolicy policy = new TicketAccessPolicy(accessControlService);

        policy.checkAssignmentAccess();

        verify(accessControlService).checkAnyRole(
                com.enterprise.ticketing.auth.domain.SystemRole.SUPPORT_AGENT,
                com.enterprise.ticketing.auth.domain.SystemRole.ADMIN
        );
    }

    private static TicketEntity ticket(Long requesterId, Long assigneeId, TicketStatus status) {
        TicketEntity ticket = new TicketEntity();
        ticket.setRequester(user(requesterId));
        if (assigneeId != null) {
            ticket.assignTo(user(assigneeId));
        }
        ReflectionTestUtils.setField(ticket, "status", status);
        return ticket;
    }

    private static UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
