package com.enterprise.ticketing.workflow.impl;

import com.enterprise.ticketing.approval.domain.ApprovalAction;
import com.enterprise.ticketing.approval.domain.ApprovalStageKey;
import com.enterprise.ticketing.approval.domain.ApprovalStatus;
import com.enterprise.ticketing.approval.entity.ApprovalEntity;
import com.enterprise.ticketing.approval.repository.ApprovalRepository;
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
import com.enterprise.ticketing.workflow.activity.ApprovalWorkflowActivities;
import com.enterprise.ticketing.workflow.dto.ApprovalDecisionOutcome;
import com.enterprise.ticketing.workflow.dto.ApprovalDecisionSignal;
import com.enterprise.ticketing.workflow.dto.ApprovalStageExecution;
import com.enterprise.ticketing.workflow.dto.ApprovalWorkflowInput;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class ApprovalWorkflowActivitiesImpl implements ApprovalWorkflowActivities {

    private static final Logger log = LoggerFactory.getLogger(ApprovalWorkflowActivitiesImpl.class);

    private final ApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final TicketService ticketService;
    private final TicketEventService ticketEventService;
    private final TelemetryService telemetryService;

    public ApprovalWorkflowActivitiesImpl(
            ApprovalRepository approvalRepository,
            UserRepository userRepository,
            TicketRepository ticketRepository,
            TicketService ticketService,
            TicketEventService ticketEventService,
            TelemetryService telemetryService
    ) {
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.ticketService = ticketService;
        this.ticketEventService = ticketEventService;
        this.telemetryService = telemetryService;
    }

    @Override
    @Transactional
    public ApprovalStageExecution openApprovalStage(ApprovalWorkflowInput input, ApprovalStageKey stageKey) {
        return telemetryService.inSpan(
                "ticketing.approval.stage.open",
                Map.of("workflow.id", input.workflowId(), "stage", stageKey.name()),
                () -> openStageInternal(input, stageKey)
        );
    }

    @Override
    @Transactional
    public ApprovalDecisionOutcome applyDecision(String workflowId, Long approvalId, ApprovalDecisionSignal signal) {
        return telemetryService.inSpan(
                "ticketing.approval.stage.decision",
                Map.of("workflow.id", workflowId, "approval.id", String.valueOf(approvalId)),
                () -> applyDecisionInternal(workflowId, approvalId, signal)
        );
    }

    @Override
    @Transactional
    public void completeApprovedWorkflow(ApprovalWorkflowInput input, long workflowDurationMs) {
        telemetryService.inSpan(
                "ticketing.approval.workflow.complete",
                Map.of("workflow.id", input.workflowId(), "result", "APPROVED"),
                () -> {
                    TicketEntity ticket = requireTicket(input.ticketId());
                    if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
                        ticketService.markInProgress(ticket.getId(), "Approval workflow completed and ticket resumed");
                    }
                    ticketEventService.recordEvent(
                            input.ticketId(),
                            TicketEventType.WORKFLOW_COMPLETED,
                            "Approval workflow completed",
                            payload(
                                    "workflowId", input.workflowId(),
                                    "aiWorkflowId", input.aiWorkflowId(),
                                    "result", ApprovalStatus.APPROVED.name()
                            ),
                            null
                    );
                    telemetryService.recordApprovalWorkflowCompleted("APPROVED", workflowDurationMs);
                }
        );
    }

    @Override
    @Transactional
    public void completeRejectedWorkflow(ApprovalWorkflowInput input, Long approvalId, long workflowDurationMs) {
        telemetryService.inSpan(
                "ticketing.approval.workflow.complete",
                Map.of("workflow.id", input.workflowId(), "result", "REJECTED"),
                () -> {
                    ticketEventService.recordEvent(
                            input.ticketId(),
                            TicketEventType.WORKFLOW_COMPLETED,
                            "Approval workflow rejected",
                            payload(
                                    "workflowId", input.workflowId(),
                                    "approvalId", approvalId,
                                    "aiWorkflowId", input.aiWorkflowId(),
                                    "result", ApprovalStatus.REJECTED.name()
                            ),
                            null
                    );
                    telemetryService.recordApprovalWorkflowCompleted("REJECTED", workflowDurationMs);
                }
        );
    }

    private ApprovalStageExecution openStageInternal(ApprovalWorkflowInput input, ApprovalStageKey stageKey) {
        ApprovalEntity existing = approvalRepository.findByWorkflowIdAndStageOrder(input.workflowId(), stageKey.getOrder()).orElse(null);
        if (existing != null) {
            telemetryService.recordApprovalRetry("activity_replay");
            return toStageExecution(existing);
        }

        TicketEntity ticket = requireTicket(input.ticketId());
        UserEntity approver = resolveApprover(stageKey);

        if (stageKey == ApprovalStageKey.LINE_MANAGER && ticket.getStatus() != TicketStatus.WAITING_APPROVAL) {
            ticketService.markWaitingApproval(ticket.getId(), "Approval workflow started after AI decision");
            ticketEventService.recordEvent(
                    ticket.getId(),
                    TicketEventType.WORKFLOW_STARTED,
                    "Approval workflow started",
                    payload(
                            "workflowId", input.workflowId(),
                            "aiWorkflowId", input.aiWorkflowId(),
                            "traceId", input.traceId(),
                            "category", input.ticketCategory()
                    ),
                    null
            );
        } else if (stageKey != ApprovalStageKey.LINE_MANAGER) {
            ticketEventService.recordEvent(
                    ticket.getId(),
                    TicketEventType.WORKFLOW_RESUMED,
                    "Approval workflow resumed",
                    payload("workflowId", input.workflowId(), "nextStage", stageKey.name()),
                    null
            );
        }

        ApprovalEntity approval = new ApprovalEntity();
        approval.setTicket(ticket);
        approval.setWorkflowId(input.workflowId());
        approval.setAiWorkflowId(input.aiWorkflowId());
        approval.setStageOrder(stageKey.getOrder());
        approval.setStageKey(stageKey);
        approval.setApprover(approver);
        approval.setStatus(ApprovalStatus.PENDING);
        approval.setRequestedAt(Instant.now());
        ApprovalEntity saved = approvalRepository.saveAndFlush(approval);

        ticketEventService.recordEvent(
                ticket.getId(),
                TicketEventType.APPROVAL_REQUESTED,
                stageKey.getDisplayName() + " created",
                payload(
                        "approvalId", saved.getId(),
                        "workflowId", input.workflowId(),
                        "stageKey", stageKey.name(),
                        "stageOrder", stageKey.getOrder(),
                        "approverId", approver.getId(),
                        "approverName", approver.getDisplayName()
                ),
                null
        );
        telemetryService.recordApprovalStageOpened(stageKey.name());
        log.info("Opened approval {} for ticket {} on stage {}", saved.getId(), ticket.getId(), stageKey.name());
        return toStageExecution(saved);
    }

    private ApprovalDecisionOutcome applyDecisionInternal(String workflowId, Long approvalId, ApprovalDecisionSignal signal) {
        ApprovalEntity approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "Approval not found: " + approvalId));
        if (!approval.getWorkflowId().equals(workflowId)) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "Approval does not belong to workflow " + workflowId);
        }

        ApprovalStatus targetStatus = signal.action() == ApprovalAction.APPROVE
                ? ApprovalStatus.APPROVED
                : ApprovalStatus.REJECTED;
        if (approval.getStatus().isFinalStatus()) {
            return new ApprovalDecisionOutcome(
                    approval.getId(),
                    approval.getStatus(),
                    approval.getStatus() == targetStatus,
                    true,
                    waitDurationMs(approval)
            );
        }

        approval.setStatus(targetStatus);
        approval.setComment(normalize(signal.comment()));
        approval.setDecisionRequestId(normalize(signal.requestId()));
        approval.setDecisionTraceId(normalize(signal.traceId()));
        approval.setDecidedAt(Instant.now());
        ApprovalEntity saved = approvalRepository.saveAndFlush(approval);

        TicketEventType eventType = targetStatus == ApprovalStatus.APPROVED
                ? TicketEventType.APPROVAL_APPROVED
                : TicketEventType.APPROVAL_REJECTED;
        ticketEventService.recordEvent(
                saved.getTicket().getId(),
                eventType,
                saved.getStageKey().getDisplayName() + " " + targetStatus.name().toLowerCase(),
                payload(
                        "approvalId", saved.getId(),
                        "workflowId", saved.getWorkflowId(),
                        "stageKey", saved.getStageKey().name(),
                        "operatorId", signal.operatorId(),
                        "operatorName", signal.operatorName(),
                        "comment", normalize(signal.comment())
                ),
                signal.operatorId()
        );

        if (targetStatus == ApprovalStatus.REJECTED && saved.getTicket().getStatus() != TicketStatus.REJECTED) {
            ticketService.markRejected(saved.getTicket().getId(), "Approval rejected at " + saved.getStageKey().getDisplayName());
        }

        long waitDurationMs = waitDurationMs(saved);
        telemetryService.recordApprovalDecision(saved.getStageKey().name(), targetStatus, waitDurationMs, false);
        log.info("Approval {} decided as {} by {}", saved.getId(), targetStatus, signal.operatorName());
        return new ApprovalDecisionOutcome(
                saved.getId(),
                saved.getStatus(),
                false,
                targetStatus == ApprovalStatus.REJECTED,
                waitDurationMs
        );
    }

    private ApprovalStageExecution toStageExecution(ApprovalEntity entity) {
        return new ApprovalStageExecution(
                entity.getId(),
                entity.getStageKey(),
                entity.getStageOrder(),
                entity.getApprover().getId(),
                entity.getApprover().getDisplayName()
        );
    }

    private UserEntity resolveApprover(ApprovalStageKey stageKey) {
        List<UserEntity> candidates = userRepository.findEnabledUsersByRole(stageKey.getResolverRole());
        if (candidates.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.COMMON_NOT_FOUND,
                    "No enabled approver found for role " + stageKey.getResolverRole()
            );
        }
        return candidates.get(0);
    }

    private TicketEntity requireTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "Ticket not found: " + ticketId));
    }

    private long waitDurationMs(ApprovalEntity approval) {
        Instant decidedAt = approval.getDecidedAt() == null ? Instant.now() : approval.getDecidedAt();
        return Duration.between(approval.getRequestedAt(), decidedAt).toMillis();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Map<String, Object> payload(Object... keyValues) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key instanceof String stringKey && value != null) {
                payload.put(stringKey, value);
            }
        }
        return payload;
    }
}
