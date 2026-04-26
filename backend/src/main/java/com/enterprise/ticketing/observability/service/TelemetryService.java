package com.enterprise.ticketing.observability.service;

import com.enterprise.ticketing.approval.domain.ApprovalStatus;
import com.enterprise.ticketing.observability.dto.DashboardMetricsResponse;
import java.util.Map;
import java.util.function.Supplier;

public interface TelemetryService {

    <T> T inSpan(String spanName, Map<String, String> tags, Supplier<T> supplier);

    void inSpan(String spanName, Map<String, String> tags, Runnable runnable);

    void recordAiOrchestrationResult(boolean success, long durationMs);

    void recordAiDecisionHandling(
            boolean requiresApproval,
            boolean needsHumanHandoff,
            boolean fallbackUsed,
            String retrievalStatus,
            boolean startedWorkflow,
            boolean manualReviewRequired,
            long durationMs
    );

    void recordApprovalWorkflowStarted();

    void recordApprovalWorkflowCompleted(String result, long durationMs);

    void recordApprovalStageOpened(String stageKey);

    void recordApprovalDecision(String stageKey, ApprovalStatus status, long waitDurationMs, boolean idempotent);

    void recordApprovalCommand(String action, String result);

    void recordApprovalFailure(String phase);

    void recordApprovalRetry(String source);

    DashboardMetricsResponse getDashboardMetrics();
}
