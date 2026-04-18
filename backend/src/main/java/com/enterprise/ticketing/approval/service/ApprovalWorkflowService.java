package com.enterprise.ticketing.approval.service;

import com.enterprise.ticketing.ai.dto.AiDecisionResult;
import com.enterprise.ticketing.approval.dto.ApprovalWorkflowLaunchResponse;

public interface ApprovalWorkflowService {

    ApprovalWorkflowLaunchResponse handleAiDecision(AiDecisionResult decisionResult);
}
