package com.enterprise.ticketing.workflow.activity;

import com.enterprise.ticketing.approval.domain.ApprovalStageKey;
import com.enterprise.ticketing.workflow.dto.ApprovalDecisionOutcome;
import com.enterprise.ticketing.workflow.dto.ApprovalDecisionSignal;
import com.enterprise.ticketing.workflow.dto.ApprovalStageExecution;
import com.enterprise.ticketing.workflow.dto.ApprovalWorkflowInput;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ApprovalWorkflowActivities {

    @ActivityMethod
    ApprovalStageExecution openApprovalStage(ApprovalWorkflowInput input, ApprovalStageKey stageKey);

    @ActivityMethod
    ApprovalDecisionOutcome applyDecision(String workflowId, Long approvalId, ApprovalDecisionSignal signal);

    @ActivityMethod
    void completeApprovedWorkflow(ApprovalWorkflowInput input, long workflowDurationMs);

    @ActivityMethod
    void completeRejectedWorkflow(ApprovalWorkflowInput input, Long approvalId, long workflowDurationMs);
}
