package com.enterprise.ticketing.workflow.impl;

import com.enterprise.ticketing.approval.domain.ApprovalStageKey;
import com.enterprise.ticketing.approval.domain.ApprovalStatus;
import com.enterprise.ticketing.workflow.ApprovalWorkflow;
import com.enterprise.ticketing.workflow.activity.ApprovalWorkflowActivities;
import com.enterprise.ticketing.workflow.dto.ApprovalDecisionOutcome;
import com.enterprise.ticketing.workflow.dto.ApprovalDecisionSignal;
import com.enterprise.ticketing.workflow.dto.ApprovalStageExecution;
import com.enterprise.ticketing.workflow.dto.ApprovalWorkflowInput;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ApprovalWorkflowImpl implements ApprovalWorkflow {

    private final ApprovalWorkflowActivities activities = Workflow.newActivityStub(
            ApprovalWorkflowActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(1))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(5).build())
                    .build()
    );

    private final Map<Long, ApprovalDecisionSignal> pendingSignals = new HashMap<>();
    private final Set<Long> processedApprovalIds = new HashSet<>();

    @Override
    public void run(ApprovalWorkflowInput input) {
        long startedAt = Workflow.currentTimeMillis();
        for (ApprovalStageKey stageKey : EnumSet.allOf(ApprovalStageKey.class)) {
            ApprovalStageExecution stageExecution = activities.openApprovalStage(input, stageKey);
            Long approvalId = stageExecution.approvalId();
            Workflow.await(() -> pendingSignals.containsKey(approvalId));
            ApprovalDecisionSignal signal = pendingSignals.remove(approvalId);
            processedApprovalIds.add(approvalId);

            ApprovalDecisionOutcome outcome = activities.applyDecision(input.workflowId(), approvalId, signal);
            if (outcome.status() == ApprovalStatus.REJECTED) {
                activities.completeRejectedWorkflow(input, approvalId, Workflow.currentTimeMillis() - startedAt);
                return;
            }
        }
        activities.completeApprovedWorkflow(input, Workflow.currentTimeMillis() - startedAt);
    }

    @Override
    public void submitDecision(ApprovalDecisionSignal signal) {
        if (signal == null || signal.approvalId() == null || processedApprovalIds.contains(signal.approvalId())) {
            return;
        }
        ApprovalDecisionSignal existing = pendingSignals.get(signal.approvalId());
        if (existing == null || existing.action() == signal.action()) {
            pendingSignals.put(signal.approvalId(), signal);
        }
    }
}
