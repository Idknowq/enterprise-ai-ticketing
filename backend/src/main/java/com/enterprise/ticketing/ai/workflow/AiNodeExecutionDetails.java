package com.enterprise.ticketing.ai.workflow;

import com.enterprise.ticketing.ai.domain.AiNodeName;

public record AiNodeExecutionDetails(
        AiNodeName nodeName,
        String providerType,
        String modelName,
        boolean fallbackUsed,
        String fallbackReason
) {
}
