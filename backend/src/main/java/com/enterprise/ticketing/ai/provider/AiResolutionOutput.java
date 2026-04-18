package com.enterprise.ticketing.ai.provider;

import java.util.List;

public record AiResolutionOutput(
        boolean requiresApproval,
        boolean needsHumanHandoff,
        String draftReply,
        List<String> suggestedActions
) {
}
