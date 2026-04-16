package com.enterprise.ticketing.ticket.dto;

import com.enterprise.ticketing.ticket.domain.TicketEventType;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Ticket timeline event payload")
public record TicketEventResponse(
        Long id,
        TicketEventType eventType,
        String summary,
        JsonNode payload,
        TicketUserSummaryResponse operator,
        Instant createdAt
) {
}
