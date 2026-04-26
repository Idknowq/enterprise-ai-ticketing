package com.enterprise.ticketing.ai.dto;

import com.enterprise.ticketing.ai.domain.AiNodeName;
import com.enterprise.ticketing.ai.domain.AiRunStatus;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Single AI node execution log")
public record AiNodeRunResponse(
        Long id,
        String workflowId,
        AiNodeName nodeName,
        AiRunStatus status,
        String providerType,
        String modelName,
        int latencyMs,
        int tokenInput,
        int tokenOutput,
        boolean fallbackUsed,
        String fallbackReason,
        String retrievalStatus,
        String resultSummary,
        JsonNode resultPayload,
        String errorMessage,
        Instant createdAt
) {
}
