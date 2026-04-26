package com.enterprise.ticketing.approval.service.impl;

import com.enterprise.ticketing.ai.dto.AiDecisionResult;
import com.enterprise.ticketing.approval.domain.ApprovalStatus;
import com.enterprise.ticketing.approval.dto.ApprovalWorkflowLaunchResponse;
import com.enterprise.ticketing.approval.entity.ApprovalEntity;
import com.enterprise.ticketing.approval.repository.ApprovalRepository;
import com.enterprise.ticketing.approval.service.ApprovalWorkflowService;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.common.util.TraceIdUtils;
import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.observability.service.TelemetryService;
import com.enterprise.ticketing.ticket.domain.TicketEventType;
import com.enterprise.ticketing.ticket.dto.TicketAiDecisionAssessment;
import com.enterprise.ticketing.ticket.service.TicketAiDecisionService;
import com.enterprise.ticketing.ticket.service.TicketEventService;
import com.enterprise.ticketing.workflow.ApprovalWorkflow;
import com.enterprise.ticketing.workflow.dto.ApprovalWorkflowInput;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ApprovalWorkflowServiceImpl implements ApprovalWorkflowService {

    private final ApprovalRepository approvalRepository;
    private final WorkflowClient workflowClient;
    private final ApplicationProperties applicationProperties;
    private final TelemetryService telemetryService;
    private final TicketAiDecisionService ticketAiDecisionService;
    private final TicketEventService ticketEventService;

    public ApprovalWorkflowServiceImpl(
            ApprovalRepository approvalRepository,
            WorkflowClient workflowClient,
            ApplicationProperties applicationProperties,
            TelemetryService telemetryService,
            TicketAiDecisionService ticketAiDecisionService,
            TicketEventService ticketEventService
    ) {
        this.approvalRepository = approvalRepository;
        this.workflowClient = workflowClient;
        this.applicationProperties = applicationProperties;
        this.telemetryService = telemetryService;
        this.ticketAiDecisionService = ticketAiDecisionService;
        this.ticketEventService = ticketEventService;
    }

    @Override
    public ApprovalWorkflowLaunchResponse handleAiDecision(AiDecisionResult decisionResult) {
        long startedAt = System.nanoTime();
        ApprovalWorkflowLaunchResponse response = telemetryService.inSpan(
                "ticketing.approval.workflow.start",
                spanTags(
                        "ticket.id", String.valueOf(decisionResult.ticketId()),
                        "requires.approval", Boolean.toString(decisionResult.requiresApproval()),
                        "needs.human.handoff", Boolean.toString(decisionResult.needsHumanHandoff()),
                        "fallback.used", Boolean.toString(decisionResult.fallbackUsed()),
                        "retrieval.status", decisionResult.retrievalStatus()
                ),
                () -> handleDecisionInternal(decisionResult)
        );
        telemetryService.recordAiDecisionHandling(
                decisionResult.requiresApproval(),
                decisionResult.needsHumanHandoff(),
                decisionResult.fallbackUsed(),
                decisionResult.retrievalStatus(),
                response.started(),
                response.manualReviewRequired(),
                Duration.ofNanos(System.nanoTime() - startedAt).toMillis()
        );
        return response;
    }

    private ApprovalWorkflowLaunchResponse handleDecisionInternal(AiDecisionResult decisionResult) {
        TicketAiDecisionAssessment assessment = ticketAiDecisionService.assessDecision(decisionResult);
        if (assessment.manualReviewRequired()) {
            ticketEventService.recordEvent(
                    decisionResult.ticketId(),
                    TicketEventType.AI_REVIEW_REQUIRED,
                    "AI decision requires manual review",
                    payload(
                            "workflowId", decisionResult.workflowId(),
                            "requiresApproval", decisionResult.requiresApproval(),
                            "needsHumanHandoff", decisionResult.needsHumanHandoff(),
                            "fallbackUsed", decisionResult.fallbackUsed(),
                            "fallbackReason", decisionResult.fallbackReason(),
                            "retrievalStatus", decisionResult.retrievalStatus(),
                            "retrievalDiagnostics", decisionResult.retrievalDiagnostics(),
                            "reasons", assessment.reasons()
                    ),
                    null
            );
            return new ApprovalWorkflowLaunchResponse(
                    decisionResult.ticketId(),
                    null,
                    false,
                    false,
                    true,
                    assessment.summary()
            );
        }

        if (!decisionResult.requiresApproval()) {
            return new ApprovalWorkflowLaunchResponse(
                    decisionResult.ticketId(),
                    null,
                    false,
                    false,
                    false,
                    "AI decision does not require approval"
            );
        }

        ApprovalEntity pendingApproval = approvalRepository.findFirstByTicketIdAndStatusOrderByStageOrderAsc(
                decisionResult.ticketId(),
                ApprovalStatus.PENDING
        ).orElse(null);
        if (pendingApproval != null) {
            telemetryService.recordApprovalRetry("duplicate_start");
            return new ApprovalWorkflowLaunchResponse(
                    decisionResult.ticketId(),
                    pendingApproval.getWorkflowId(),
                    false,
                    true,
                    false,
                    "Ticket is already waiting for approval"
            );
        }

        String workflowId = workflowId(decisionResult);
        WorkflowOptions workflowOptions = WorkflowOptions.newBuilder()
                .setTaskQueue(applicationProperties.getTemporal().getTaskQueue())
                .setWorkflowId(workflowId)
                .setWorkflowRunTimeout(Duration.ofDays(7))
                .build();

        try {
            ApprovalWorkflow workflow = workflowClient.newWorkflowStub(ApprovalWorkflow.class, workflowOptions);
            WorkflowClient.start(
                    workflow::run,
                    new ApprovalWorkflowInput(
                            decisionResult.ticketId(),
                            workflowId,
                            decisionResult.workflowId(),
                            decisionResult.category(),
                            TraceIdUtils.currentTraceId()
                    )
            );
            telemetryService.recordApprovalWorkflowStarted();
            return new ApprovalWorkflowLaunchResponse(
                    decisionResult.ticketId(),
                    workflowId,
                    true,
                    false,
                    false,
                    "Approval workflow started"
            );
        } catch (WorkflowExecutionAlreadyStarted exception) {
            telemetryService.recordApprovalRetry("workflow_already_started");
            return new ApprovalWorkflowLaunchResponse(
                    decisionResult.ticketId(),
                    workflowId,
                    false,
                    true,
                    false,
                    "Approval workflow already started"
            );
        } catch (RuntimeException exception) {
            telemetryService.recordApprovalFailure("start");
            throw new BusinessException(
                    ErrorCode.COMMON_SERVICE_UNAVAILABLE,
                    "Failed to start approval workflow for ticket " + decisionResult.ticketId()
            );
        }
    }

    private String workflowId(AiDecisionResult decisionResult) {
        return "approval-ticket-" + decisionResult.ticketId() + "-" + decisionResult.workflowId();
    }

    private Map<String, Object> payload(Object... values) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            Object value = values[index + 1];
            if (value == null) {
                continue;
            }
            if (value instanceof String stringValue && !StringUtils.hasText(stringValue)) {
                continue;
            }
            payload.put((String) values[index], value);
        }
        return payload;
    }

    private Map<String, String> spanTags(String... values) {
        Map<String, String> tags = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            String value = values[index + 1];
            if (!StringUtils.hasText(value)) {
                continue;
            }
            tags.put(values[index], value);
        }
        return tags;
    }
}
