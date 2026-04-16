package com.enterprise.ticketing.ticket.dto;

import com.enterprise.ticketing.ticket.domain.TicketPriority;
import com.enterprise.ticketing.ticket.domain.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Ticket summary payload")
public record TicketSummaryResponse(
        Long id,
        String title,
        String category,
        TicketPriority priority,
        TicketStatus status,
        TicketUserSummaryResponse requester,
        TicketUserSummaryResponse assignee,
        Instant createdAt,
        Instant updatedAt
) {
}
