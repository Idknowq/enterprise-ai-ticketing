package com.enterprise.ticketing.ai.dto;

import com.enterprise.ticketing.ticket.domain.TicketPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(description = "Structured AI decision result for a ticket")
public record AiDecisionResult(
        String workflowId,
        Long ticketId,
        String category,
        TicketPriority priority,
        double confidence,
        boolean requiresApproval,
        boolean needsHumanHandoff,
        String draftReply,
        List<String> suggestedActions,
        Map<String, String> extractedFields,
        List<AiCitation> citations,
        Instant generatedAt
) {
}
