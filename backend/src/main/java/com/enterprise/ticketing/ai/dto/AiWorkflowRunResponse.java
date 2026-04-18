package com.enterprise.ticketing.ai.dto;

import com.enterprise.ticketing.ai.domain.AiRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Grouped AI workflow execution history for a ticket")
public record AiWorkflowRunResponse(
        String workflowId,
        AiRunStatus status,
        Instant startedAt,
        AiDecisionResult finalDecision,
        List<AiNodeRunResponse> nodes
) {
}
