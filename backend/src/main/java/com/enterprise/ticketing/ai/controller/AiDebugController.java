package com.enterprise.ticketing.ai.controller;

import com.enterprise.ticketing.ai.dto.AiDecisionResult;
import com.enterprise.ticketing.ai.dto.AiWorkflowRunResponse;
import com.enterprise.ticketing.ai.service.AiOrchestrationService;
import com.enterprise.ticketing.approval.service.ApprovalWorkflowService;
import com.enterprise.ticketing.common.api.Result;
import com.enterprise.ticketing.common.util.TraceIdUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "AI orchestration debug APIs")
@Validated
@RestController
@RequestMapping("${app.api-base-path:/api}/ai/tickets")
public class AiDebugController {

    private final AiOrchestrationService aiOrchestrationService;
    private final ApprovalWorkflowService approvalWorkflowService;

    public AiDebugController(
            AiOrchestrationService aiOrchestrationService,
            ApprovalWorkflowService approvalWorkflowService
    ) {
        this.aiOrchestrationService = aiOrchestrationService;
        this.approvalWorkflowService = approvalWorkflowService;
    }

    @Operation(
            summary = "Run AI orchestration for a ticket",
            description = "Debug endpoint that synchronously runs the classifier, extractor, retrieval, and resolution chain. If the AI result requires approval, the approval workflow is started immediately."
    )
    @PostMapping("/{id}/run")
    public Result<AiDecisionResult> runForTicket(@PathVariable("id") Long ticketId) {
        AiDecisionResult decisionResult = aiOrchestrationService.runForTicket(ticketId);
        approvalWorkflowService.handleAiDecision(decisionResult);
        return Result.success(decisionResult, TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "List AI workflow runs for a ticket", description = "Return grouped ai_runs history for the specified ticket.")
    @GetMapping("/{id}/runs")
    public Result<List<AiWorkflowRunResponse>> listRuns(@PathVariable("id") Long ticketId) {
        return Result.success(aiOrchestrationService.listRuns(ticketId), TraceIdUtils.currentTraceId());
    }
}
