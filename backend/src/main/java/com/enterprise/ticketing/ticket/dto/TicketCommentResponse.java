package com.enterprise.ticketing.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Ticket comment payload")
public record TicketCommentResponse(
        Long id,
        String content,
        TicketUserSummaryResponse author,
        Instant createdAt,
        Instant updatedAt
) {
}
