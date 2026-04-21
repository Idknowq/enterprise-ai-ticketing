package com.enterprise.ticketing.workflow.impl;

import com.enterprise.ticketing.approval.domain.ApprovalAction;
import com.enterprise.ticketing.approval.domain.ApprovalStageKey;
import com.enterprise.ticketing.approval.domain.ApprovalStatus;
import com.enterprise.ticketing.approval.entity.ApprovalEntity;
import com.enterprise.ticketing.approval.repository.ApprovalRepository;
import com.enterprise.ticketing.auth.domain.SystemRole;
import com.enterprise.ticketing.auth.entity.RoleEntity;
import com.enterprise.ticketing.auth.entity.UserEntity;
import com.enterprise.ticketing.auth.repository.UserRepository;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.observability.service.TelemetryService;
import com.enterprise.ticketing.ticket.domain.TicketEventType;
import com.enterprise.ticketing.ticket.domain.TicketStatus;
import com.enterprise.ticketing.ticket.entity.TicketEntity;
import com.enterprise.ticketing.ticket.repository.TicketRepository;
import com.enterprise.ticketing.ticket.service.TicketEventService;
import com.enterprise.ticketing.ticket.service.TicketService;
import com.enterprise.ticketing.workflow.dto.ApprovalDecisionOutcome;
import com.enterprise.ticketing.workflow.dto.ApprovalDecisionSignal;
import com.enterprise.ticketing.workflow.dto.ApprovalStageExecution;
import com.enterprise.ticketing.workflow.dto.ApprovalWorkflowInput;
import java.time.Instant;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalWorkflowActivitiesImplTest {

    @Mock
    private ApprovalRepository approvalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketService ticketService;

    @Mock
    private TicketEventService ticketEventService;

    @Mock
    private TelemetryService telemetryService;

    private ApprovalWorkflowActivitiesImpl activities;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        when(telemetryService.inSpan(anyString(), anyMap(), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(2)).get());
        activities = new ApprovalWorkflowActivitiesImpl(
                approvalRepository,
                userRepository,
                ticketRepository,
                ticketService,
                ticketEventService,
                telemetryService
        );
    }

    @Test
    void openFirstStageMarksTicketWaitingApprovalAndCreatesApproval() {
        ApprovalWorkflowInput input = input();
        TicketEntity ticket = ticket(20L, TicketStatus.OPEN);
        UserEntity approver = user(3L, "Approver Demo", SystemRole.APPROVER);

        when(approvalRepository.findByWorkflowIdAndStageOrder("workflow-20", 1)).thenReturn(Optional.empty());
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(ticket));
        when(userRepository.findEnabledUsersByRole(SystemRole.APPROVER)).thenReturn(List.of(approver));
        when(approvalRepository.saveAndFlush(any(ApprovalEntity.class))).thenAnswer(invocation -> {
            ApprovalEntity approval = invocation.getArgument(0);
            ReflectionTestUtils.setField(approval, "id", 101L);
            return approval;
        });

        ApprovalStageExecution result = activities.openApprovalStage(input, ApprovalStageKey.LINE_MANAGER);

        assertThat(result.approvalId()).isEqualTo(101L);
        assertThat(result.stageKey()).isEqualTo(ApprovalStageKey.LINE_MANAGER);
        assertThat(result.approverId()).isEqualTo(3L);

        verify(ticketService).markWaitingApproval(20L, "Approval workflow started after AI decision");
        verify(ticketEventService).recordEvent(
                eq(20L),
                eq(TicketEventType.WORKFLOW_STARTED),
                eq("Approval workflow started"),
                anyMap(),
                eq(null)
        );
        verify(ticketEventService).recordEvent(
                eq(20L),
                eq(TicketEventType.APPROVAL_REQUESTED),
                eq("Line manager approval created"),
                anyMap(),
                eq(null)
        );
        verify(telemetryService).recordApprovalStageOpened("LINE_MANAGER");
    }

    @Test
    void openStageReplayReturnsExistingApprovalWithoutSideEffects() {
        UserEntity approver = user(3L, "Approver Demo", SystemRole.APPROVER);
        ApprovalEntity existing = approval(101L, ticket(20L, TicketStatus.WAITING_APPROVAL), approver, ApprovalStatus.PENDING);
        when(approvalRepository.findByWorkflowIdAndStageOrder("workflow-20", 1)).thenReturn(Optional.of(existing));

        ApprovalStageExecution result = activities.openApprovalStage(input(), ApprovalStageKey.LINE_MANAGER);

        assertThat(result.approvalId()).isEqualTo(101L);
        verify(telemetryService).recordApprovalRetry("activity_replay");
        verify(ticketService, never()).markWaitingApproval(any(), anyString());
        verify(approvalRepository, never()).saveAndFlush(any(ApprovalEntity.class));
    }

    @Test
    void openSecondStageRecordsWorkflowResumedEvent() {
        ApprovalWorkflowInput input = input();
        TicketEntity ticket = ticket(20L, TicketStatus.WAITING_APPROVAL);
        UserEntity admin = user(1L, "Admin Demo", SystemRole.ADMIN);

        when(approvalRepository.findByWorkflowIdAndStageOrder("workflow-20", 2)).thenReturn(Optional.empty());
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(ticket));
        when(userRepository.findEnabledUsersByRole(SystemRole.ADMIN)).thenReturn(List.of(admin));
        when(approvalRepository.saveAndFlush(any(ApprovalEntity.class))).thenAnswer(invocation -> {
            ApprovalEntity approval = invocation.getArgument(0);
            ReflectionTestUtils.setField(approval, "id", 102L);
            return approval;
        });

        ApprovalStageExecution result = activities.openApprovalStage(input, ApprovalStageKey.SYSTEM_ADMIN);

        assertThat(result.stageKey()).isEqualTo(ApprovalStageKey.SYSTEM_ADMIN);
        verify(ticketService, never()).markWaitingApproval(any(), anyString());
        verify(ticketEventService).recordEvent(
                eq(20L),
                eq(TicketEventType.WORKFLOW_RESUMED),
                eq("Approval workflow resumed"),
                anyMap(),
                eq(null)
        );
    }

    @Test
    void applyApproveDecisionFinalizesApprovalAndRecordsApprovedEvent() {
        UserEntity approver = user(3L, "Approver Demo", SystemRole.APPROVER);
        TicketEntity ticket = ticket(20L, TicketStatus.WAITING_APPROVAL);
        ApprovalEntity approval = approval(101L, ticket, approver, ApprovalStatus.PENDING);
        when(approvalRepository.findById(101L)).thenReturn(Optional.of(approval));
        when(approvalRepository.saveAndFlush(any(ApprovalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalDecisionOutcome outcome = activities.applyDecision(
                "workflow-20",
                101L,
                new ApprovalDecisionSignal(101L, ApprovalAction.APPROVE, 3L, "Approver Demo", " ok ", " request-1 ", " trace-1 ")
        );

        assertThat(outcome.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(outcome.idempotent()).isFalse();
        assertThat(outcome.terminal()).isFalse();
        assertThat(approval.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(approval.getComment()).isEqualTo("ok");
        assertThat(approval.getDecisionRequestId()).isEqualTo("request-1");
        assertThat(approval.getDecisionTraceId()).isEqualTo("trace-1");
        assertThat(approval.getDecidedAt()).isNotNull();

        verify(ticketEventService).recordEvent(
                eq(20L),
                eq(TicketEventType.APPROVAL_APPROVED),
                eq("Line manager approval approved"),
                anyMap(),
                eq(3L)
        );
        verify(ticketService, never()).markRejected(any(), anyString());
        verify(telemetryService).recordApprovalDecision(eq("LINE_MANAGER"), eq(ApprovalStatus.APPROVED), anyLong(), eq(false));
    }

    @Test
    void applyRejectDecisionMarksTicketRejectedAndReturnsTerminalOutcome() {
        UserEntity approver = user(3L, "Approver Demo", SystemRole.APPROVER);
        TicketEntity ticket = ticket(20L, TicketStatus.WAITING_APPROVAL);
        ApprovalEntity approval = approval(101L, ticket, approver, ApprovalStatus.PENDING);
        when(approvalRepository.findById(101L)).thenReturn(Optional.of(approval));
        when(approvalRepository.saveAndFlush(any(ApprovalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalDecisionOutcome outcome = activities.applyDecision(
                "workflow-20",
                101L,
                new ApprovalDecisionSignal(101L, ApprovalAction.REJECT, 3L, "Approver Demo", " denied ", " request-2 ", " trace-2 ")
        );

        assertThat(outcome.status()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(outcome.terminal()).isTrue();
        verify(ticketEventService).recordEvent(
                eq(20L),
                eq(TicketEventType.APPROVAL_REJECTED),
                eq("Line manager approval rejected"),
                anyMap(),
                eq(3L)
        );
        verify(ticketService).markRejected(20L, "Approval rejected at Line manager approval");
    }

    @Test
    void applyDecisionForFinalApprovalIsIdempotentAndDoesNotSaveAgain() {
        UserEntity approver = user(3L, "Approver Demo", SystemRole.APPROVER);
        ApprovalEntity approval = approval(101L, ticket(20L, TicketStatus.WAITING_APPROVAL), approver, ApprovalStatus.APPROVED);
        approval.setDecidedAt(Instant.parse("2026-04-20T10:00:00Z"));
        when(approvalRepository.findById(101L)).thenReturn(Optional.of(approval));

        ApprovalDecisionOutcome outcome = activities.applyDecision(
                "workflow-20",
                101L,
                new ApprovalDecisionSignal(101L, ApprovalAction.APPROVE, 3L, "Approver Demo", " ok ", " request-1 ", " trace-1 ")
        );

        assertThat(outcome.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(outcome.idempotent()).isTrue();
        verify(approvalRepository, never()).saveAndFlush(any(ApprovalEntity.class));
        verify(ticketEventService, never()).recordEvent(any(), any(), anyString(), anyMap(), any());
    }

    @Test
    void applyDecisionRejectsApprovalFromAnotherWorkflow() {
        UserEntity approver = user(3L, "Approver Demo", SystemRole.APPROVER);
        ApprovalEntity approval = approval(101L, ticket(20L, TicketStatus.WAITING_APPROVAL), approver, ApprovalStatus.PENDING);
        when(approvalRepository.findById(101L)).thenReturn(Optional.of(approval));

        assertThatThrownBy(() -> activities.applyDecision(
                "another-workflow",
                101L,
                new ApprovalDecisionSignal(101L, ApprovalAction.APPROVE, 3L, "Approver Demo", null, null, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
    }

    @Test
    void openStageFailsWhenNoApproverExists() {
        when(approvalRepository.findByWorkflowIdAndStageOrder("workflow-20", 1)).thenReturn(Optional.empty());
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(ticket(20L, TicketStatus.OPEN)));
        when(userRepository.findEnabledUsersByRole(SystemRole.APPROVER)).thenReturn(List.of());

        assertThatThrownBy(() -> activities.openApprovalStage(input(), ApprovalStageKey.LINE_MANAGER))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    private static ApprovalWorkflowInput input() {
        return new ApprovalWorkflowInput(20L, "workflow-20", "ai-20", "ACCESS_REQUEST", "trace-20");
    }

    private static TicketEntity ticket(Long id, TicketStatus status) {
        TicketEntity ticket = new TicketEntity();
        ReflectionTestUtils.setField(ticket, "id", id);
        ReflectionTestUtils.setField(ticket, "status", status);
        ticket.setTitle("Ticket " + id);
        ticket.setDescription("Description " + id);
        return ticket;
    }

    private static ApprovalEntity approval(Long id, TicketEntity ticket, UserEntity approver, ApprovalStatus status) {
        ApprovalEntity approval = new ApprovalEntity();
        ReflectionTestUtils.setField(approval, "id", id);
        approval.setTicket(ticket);
        approval.setWorkflowId("workflow-20");
        approval.setAiWorkflowId("ai-20");
        approval.setStageOrder(ApprovalStageKey.LINE_MANAGER.getOrder());
        approval.setStageKey(ApprovalStageKey.LINE_MANAGER);
        approval.setApprover(approver);
        approval.setStatus(status);
        approval.setRequestedAt(Instant.parse("2026-04-20T09:00:00Z"));
        return approval;
    }

    private static UserEntity user(Long id, String displayName, SystemRole role) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setCode(role);
        roleEntity.setName(role.name());

        UserEntity user = new UserEntity();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(displayName.toLowerCase().replace(" ", "."));
        user.setDisplayName(displayName);
        user.setPasswordHash("{noop}password");
        user.setEnabled(true);
        user.setRoles(Set.of(roleEntity));
        return user;
    }
}
