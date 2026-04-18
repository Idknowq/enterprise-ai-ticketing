package com.enterprise.ticketing.approval.service.impl;

import com.enterprise.ticketing.approval.domain.ApprovalAction;
import com.enterprise.ticketing.approval.domain.ApprovalStatus;
import com.enterprise.ticketing.approval.dto.ApprovalCommandRequest;
import com.enterprise.ticketing.approval.dto.ApprovalDecisionResponse;
import com.enterprise.ticketing.approval.entity.ApprovalEntity;
import com.enterprise.ticketing.approval.repository.ApprovalRepository;
import com.enterprise.ticketing.approval.service.ApprovalCommandService;
import com.enterprise.ticketing.auth.context.UserContext;
import com.enterprise.ticketing.auth.domain.SystemRole;
import com.enterprise.ticketing.auth.security.UserPrincipal;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.common.util.TraceIdUtils;
import com.enterprise.ticketing.observability.service.TelemetryService;
import com.enterprise.ticketing.workflow.ApprovalWorkflow;
import com.enterprise.ticketing.workflow.dto.ApprovalDecisionSignal;
import io.temporal.client.WorkflowClient;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ApprovalCommandServiceImpl implements ApprovalCommandService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalCommandServiceImpl.class);

    private final ApprovalRepository approvalRepository;
    private final UserContext userContext;
    private final WorkflowClient workflowClient;
    private final TelemetryService telemetryService;

    public ApprovalCommandServiceImpl(
            ApprovalRepository approvalRepository,
            UserContext userContext,
            WorkflowClient workflowClient,
            TelemetryService telemetryService
    ) {
        this.approvalRepository = approvalRepository;
        this.userContext = userContext;
        this.workflowClient = workflowClient;
        this.telemetryService = telemetryService;
    }

    @Override
    public ApprovalDecisionResponse approve(Long approvalId, ApprovalCommandRequest request) {
        return submitDecision(approvalId, request, ApprovalAction.APPROVE);
    }

    @Override
    public ApprovalDecisionResponse reject(Long approvalId, ApprovalCommandRequest request) {
        return submitDecision(approvalId, request, ApprovalAction.REJECT);
    }

    private ApprovalDecisionResponse submitDecision(Long approvalId, ApprovalCommandRequest request, ApprovalAction action) {
        return telemetryService.inSpan(
                "ticketing.approval.command",
                Map.of("approval.id", String.valueOf(approvalId), "action", action.name()),
                () -> submitDecisionInternal(approvalId, request, action)
        );
    }

    private ApprovalDecisionResponse submitDecisionInternal(
            Long approvalId,
            ApprovalCommandRequest request,
            ApprovalAction action
    ) {
        ApprovalEntity approval = requireApproval(approvalId);
        UserPrincipal operator = userContext.requireCurrentUser();
        checkApprovalAccess(approval, operator);

        ApprovalStatus targetStatus = targetStatus(action);
        if (approval.getStatus().isFinalStatus()) {
            if (approval.getStatus() == targetStatus) {
                telemetryService.recordApprovalCommand(action.name(), "IDEMPOTENT");
                telemetryService.recordApprovalRetry("duplicate_callback");
                return toDecisionResponse(approval, true);
            }
            telemetryService.recordApprovalCommand(action.name(), "CONFLICT");
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT,
                    "Approval " + approvalId + " was already decided as " + approval.getStatus()
            );
        }

        ApprovalDecisionSignal signal = new ApprovalDecisionSignal(
                approvalId,
                action,
                operator.getId(),
                operator.getDisplayName(),
                normalize(request == null ? null : request.comment()),
                normalize(request == null ? null : request.requestId()),
                TraceIdUtils.currentTraceId()
        );

        try {
            workflowClient.newWorkflowStub(ApprovalWorkflow.class, approval.getWorkflowId()).submitDecision(signal);
            telemetryService.recordApprovalCommand(action.name(), "ACCEPTED");
        } catch (RuntimeException exception) {
            telemetryService.recordApprovalFailure("signal");
            telemetryService.recordApprovalCommand(action.name(), "FAILED");
            log.error("Failed to signal approval workflow {}", approval.getWorkflowId(), exception);
            throw new BusinessException(
                    ErrorCode.COMMON_SERVICE_UNAVAILABLE,
                    "Approval workflow is unavailable for workflow " + approval.getWorkflowId()
            );
        }

        ApprovalEntity updated = waitForDecision(approvalId, targetStatus);
        if (updated.getStatus().isFinalStatus() && updated.getStatus() != targetStatus) {
            telemetryService.recordApprovalCommand(action.name(), "CONFLICT");
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT,
                    "Approval " + approvalId + " was finalized as " + updated.getStatus()
            );
        }
        return toDecisionResponse(updated, false);
    }

    private ApprovalEntity waitForDecision(Long approvalId, ApprovalStatus targetStatus) {
        ApprovalEntity latest = null;
        for (int i = 0; i < 30; i++) {
            latest = requireApproval(approvalId);
            if (latest.getStatus().isFinalStatus() || latest.getStatus() == targetStatus) {
                return latest;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return latest;
            }
        }
        return latest == null ? requireApproval(approvalId) : latest;
    }

    private void checkApprovalAccess(ApprovalEntity approval, UserPrincipal operator) {
        boolean admin = operator.getRoles().contains(SystemRole.ADMIN);
        if (admin) {
            return;
        }
        if (!approval.getApprover().getId().equals(operator.getId())) {
            throw new BusinessException(
                    ErrorCode.COMMON_FORBIDDEN,
                    "Current user is not allowed to decide approval " + approval.getId()
            );
        }
    }

    private ApprovalEntity requireApproval(Long approvalId) {
        return approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "Approval not found: " + approvalId));
    }

    private ApprovalStatus targetStatus(ApprovalAction action) {
        return action == ApprovalAction.APPROVE ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED;
    }

    private ApprovalDecisionResponse toDecisionResponse(ApprovalEntity entity, boolean idempotent) {
        return new ApprovalDecisionResponse(
                entity.getId(),
                entity.getTicket().getId(),
                entity.getWorkflowId(),
                entity.getStageOrder(),
                entity.getStageKey().name(),
                entity.getStatus(),
                entity.getComment(),
                idempotent,
                entity.getRequestedAt(),
                entity.getDecidedAt()
        );
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
