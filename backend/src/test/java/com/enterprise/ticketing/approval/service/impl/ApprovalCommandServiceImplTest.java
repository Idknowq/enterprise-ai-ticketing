package com.enterprise.ticketing.approval.service.impl;

import com.enterprise.ticketing.approval.domain.ApprovalAction;
import com.enterprise.ticketing.approval.domain.ApprovalStageKey;
import com.enterprise.ticketing.approval.domain.ApprovalStatus;
import com.enterprise.ticketing.approval.dto.ApprovalCommandRequest;
import com.enterprise.ticketing.approval.dto.ApprovalDecisionResponse;
import com.enterprise.ticketing.approval.entity.ApprovalEntity;
import com.enterprise.ticketing.approval.repository.ApprovalRepository;
import com.enterprise.ticketing.auth.context.UserContext;
import com.enterprise.ticketing.auth.domain.SystemRole;
import com.enterprise.ticketing.auth.entity.RoleEntity;
import com.enterprise.ticketing.auth.entity.UserEntity;
import com.enterprise.ticketing.auth.security.UserPrincipal;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.observability.service.TelemetryService;
import com.enterprise.ticketing.ticket.entity.TicketEntity;
import com.enterprise.ticketing.workflow.ApprovalWorkflow;
import com.enterprise.ticketing.workflow.dto.ApprovalDecisionSignal;
import io.temporal.client.WorkflowClient;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalCommandServiceImplTest {

    @Mock
    private ApprovalRepository approvalRepository;

    @Mock
    private UserContext userContext;

    @Mock
    private WorkflowClient workflowClient;

    @Mock
    private ApprovalWorkflow approvalWorkflow;

    @Mock
    private TelemetryService telemetryService;

    private ApprovalCommandServiceImpl service;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        when(telemetryService.inSpan(anyString(), anyMap(), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(2)).get());
        service = new ApprovalCommandServiceImpl(
                approvalRepository,
                userContext,
                workflowClient,
                telemetryService
        );
    }

    @Test
    void approveSignalsTemporalWorkflowAndReturnsUpdatedDecision() {
        UserEntity approver = user(3L, "approver01", "Approver Demo", SystemRole.APPROVER);
        ApprovalEntity pending = approval(11L, 20L, approver, ApprovalStatus.PENDING);
        ApprovalEntity approved = approval(11L, 20L, approver, ApprovalStatus.APPROVED);
        approved.setComment("looks good");
        approved.setDecidedAt(Instant.parse("2026-04-20T10:00:00Z"));

        when(approvalRepository.findById(11L)).thenReturn(Optional.of(pending), Optional.of(approved));
        when(userContext.requireCurrentUser()).thenReturn(UserPrincipal.from(approver));
        when(workflowClient.newWorkflowStub(ApprovalWorkflow.class, "workflow-20")).thenReturn(approvalWorkflow);

        ApprovalDecisionResponse response = service.approve(
                11L,
                new ApprovalCommandRequest("  looks good  ", " request-1 ")
        );

        ArgumentCaptor<ApprovalDecisionSignal> signalCaptor = ArgumentCaptor.forClass(ApprovalDecisionSignal.class);
        verify(approvalWorkflow).submitDecision(signalCaptor.capture());
        ApprovalDecisionSignal signal = signalCaptor.getValue();
        assertThat(signal.approvalId()).isEqualTo(11L);
        assertThat(signal.action()).isEqualTo(ApprovalAction.APPROVE);
        assertThat(signal.operatorId()).isEqualTo(3L);
        assertThat(signal.comment()).isEqualTo("looks good");
        assertThat(signal.requestId()).isEqualTo("request-1");

        assertThat(response.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(response.idempotent()).isFalse();
        verify(telemetryService).recordApprovalCommand("APPROVE", "ACCEPTED");
    }

    @Test
    void approveFinalApprovalWithSameStatusIsIdempotentAndDoesNotSignalWorkflow() {
        UserEntity approver = user(3L, "approver01", "Approver Demo", SystemRole.APPROVER);
        ApprovalEntity approval = approval(11L, 20L, approver, ApprovalStatus.APPROVED);

        when(approvalRepository.findById(11L)).thenReturn(Optional.of(approval));
        when(userContext.requireCurrentUser()).thenReturn(UserPrincipal.from(approver));

        ApprovalDecisionResponse response = service.approve(11L, new ApprovalCommandRequest(null, "duplicate"));

        assertThat(response.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(response.idempotent()).isTrue();
        verify(workflowClient, never()).newWorkflowStub(ApprovalWorkflow.class, "workflow-20");
        verify(telemetryService).recordApprovalCommand("APPROVE", "IDEMPOTENT");
        verify(telemetryService).recordApprovalRetry("duplicate_callback");
    }

    @Test
    void rejectFinalApprovalWithDifferentStatusThrowsConflict() {
        UserEntity approver = user(3L, "approver01", "Approver Demo", SystemRole.APPROVER);
        ApprovalEntity approval = approval(11L, 20L, approver, ApprovalStatus.APPROVED);

        when(approvalRepository.findById(11L)).thenReturn(Optional.of(approval));
        when(userContext.requireCurrentUser()).thenReturn(UserPrincipal.from(approver));

        assertThatThrownBy(() -> service.reject(11L, new ApprovalCommandRequest("no", "request-2")))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);

        verify(workflowClient, never()).newWorkflowStub(ApprovalWorkflow.class, "workflow-20");
        verify(telemetryService).recordApprovalCommand("REJECT", "CONFLICT");
    }

    @Test
    void nonAdminCannotDecideApprovalAssignedToAnotherUser() {
        UserEntity approver = user(3L, "approver01", "Approver Demo", SystemRole.APPROVER);
        UserEntity otherApprover = user(4L, "approver02", "Other Approver", SystemRole.APPROVER);
        ApprovalEntity approval = approval(11L, 20L, approver, ApprovalStatus.PENDING);

        when(approvalRepository.findById(11L)).thenReturn(Optional.of(approval));
        when(userContext.requireCurrentUser()).thenReturn(UserPrincipal.from(otherApprover));

        assertThatThrownBy(() -> service.approve(11L, new ApprovalCommandRequest(null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_FORBIDDEN);

        verify(workflowClient, never()).newWorkflowStub(ApprovalWorkflow.class, "workflow-20");
    }

    @Test
    void adminCanDecideApprovalAssignedToAnotherUser() {
        UserEntity approver = user(3L, "approver01", "Approver Demo", SystemRole.APPROVER);
        UserEntity admin = user(1L, "admin01", "Admin Demo", SystemRole.ADMIN);
        ApprovalEntity pending = approval(11L, 20L, approver, ApprovalStatus.PENDING);
        ApprovalEntity rejected = approval(11L, 20L, approver, ApprovalStatus.REJECTED);

        when(approvalRepository.findById(11L)).thenReturn(Optional.of(pending), Optional.of(rejected));
        when(userContext.requireCurrentUser()).thenReturn(UserPrincipal.from(admin));
        when(workflowClient.newWorkflowStub(ApprovalWorkflow.class, "workflow-20")).thenReturn(approvalWorkflow);

        ApprovalDecisionResponse response = service.reject(11L, new ApprovalCommandRequest("deny", "request-3"));

        assertThat(response.status()).isEqualTo(ApprovalStatus.REJECTED);
        verify(approvalWorkflow).submitDecision(any(ApprovalDecisionSignal.class));
    }

    private static ApprovalEntity approval(Long approvalId, Long ticketId, UserEntity approver, ApprovalStatus status) {
        TicketEntity ticket = new TicketEntity();
        ReflectionTestUtils.setField(ticket, "id", ticketId);

        ApprovalEntity approval = new ApprovalEntity();
        ReflectionTestUtils.setField(approval, "id", approvalId);
        approval.setTicket(ticket);
        approval.setWorkflowId("workflow-" + ticketId);
        approval.setAiWorkflowId("ai-" + ticketId);
        approval.setStageOrder(ApprovalStageKey.LINE_MANAGER.getOrder());
        approval.setStageKey(ApprovalStageKey.LINE_MANAGER);
        approval.setApprover(approver);
        approval.setStatus(status);
        approval.setRequestedAt(Instant.parse("2026-04-20T09:00:00Z"));
        return approval;
    }

    private static UserEntity user(Long id, String username, String displayName, SystemRole role) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setCode(role);
        roleEntity.setName(role.name());

        UserEntity user = new UserEntity();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPasswordHash("{noop}password");
        user.setEnabled(true);
        user.setRoles(Set.of(roleEntity));
        return user;
    }
}
