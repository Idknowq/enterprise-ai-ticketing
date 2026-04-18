package com.enterprise.ticketing.workflow;

import com.enterprise.ticketing.workflow.dto.ApprovalDecisionSignal;
import com.enterprise.ticketing.workflow.dto.ApprovalWorkflowInput;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ApprovalWorkflow {

    @WorkflowMethod
    void run(ApprovalWorkflowInput input);

    @SignalMethod
    void submitDecision(ApprovalDecisionSignal signal);
}
