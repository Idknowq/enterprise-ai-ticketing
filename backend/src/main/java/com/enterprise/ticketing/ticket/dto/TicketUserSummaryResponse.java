package com.enterprise.ticketing.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lightweight user summary embedded in ticket responses")
public record TicketUserSummaryResponse(
        Long id,
        String username,
        String displayName,
        String department
) {
}
