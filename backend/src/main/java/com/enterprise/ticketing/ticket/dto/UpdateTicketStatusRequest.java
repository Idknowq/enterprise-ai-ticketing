package com.enterprise.ticketing.ticket.dto;

import com.enterprise.ticketing.ticket.domain.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Update ticket status request")
public record UpdateTicketStatusRequest(
        @Schema(description = "Target ticket status")
        @NotNull
        TicketStatus status,

        @Schema(description = "Optional status note", example = "Issue reproduced and fixed by refreshing the user certificate.")
        @Size(max = 255)
        String reason
) {
}
