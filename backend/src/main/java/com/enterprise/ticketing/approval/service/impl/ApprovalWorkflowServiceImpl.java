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
import com.enterprise.ticketing.workflow.ApprovalWorkflow;
import com.enterprise.ticketing.workflow.dto.ApprovalWorkflowInput;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ApprovalWorkflowServiceImpl implements ApprovalWorkflowService {

    private final ApprovalRepository approvalRepository;
    private final WorkflowClient workflowClient;
    private final ApplicationProperties applicationProperties;
    private final TelemetryService telemetryService;

    public ApprovalWorkflowServiceImpl(
            ApprovalRepository approvalRepository,
            WorkflowClient workflowClient,
            ApplicationProperties applicationProperties,
            TelemetryService telemetryService
    ) {
        this.approvalRepository = approvalRepository;
        this.workflowClient = workflowClient;
        this.applicationProperties = applicationProperties;
        this.telemetryService = telemetryService;
    }

    @Override
    public ApprovalWorkflowLaunchResponse handleAiDecision(AiDecisionResult decisionResult) {
        long startedAt = System.nanoTime();
        ApprovalWorkflowLaunchResponse response = telemetryService.inSpan(
                "ticketing.approval.workflow.start",
                Map.of("ticket.id", String.valueOf(decisionResult.ticketId())),
                () -> handleDecisionInternal(decisionResult)
        );
        telemetryService.recordAiDecisionHandling(
                decisionResult.requiresApproval(),
                response.started(),
                Duration.ofNanos(System.nanoTime() - startedAt).toMillis()
        );
        return response;
    }

    private ApprovalWorkflowLaunchResponse handleDecisionInternal(AiDecisionResult decisionResult) {
        if (!decisionResult.requiresApproval()) {
            return new ApprovalWorkflowLaunchResponse(
                    decisionResult.ticketId(),
                    null,
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
                    "Approval workflow started"
            );
        } catch (WorkflowExecutionAlreadyStarted exception) {
            telemetryService.recordApprovalRetry("workflow_already_started");
            return new ApprovalWorkflowLaunchResponse(
                    decisionResult.ticketId(),
                    workflowId,
                    false,
                    true,
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
}
