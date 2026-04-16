package com.enterprise.ticketing.ticket.dto;

import com.enterprise.ticketing.ticket.domain.TicketPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Create ticket request")
public record CreateTicketRequest(
        @Schema(description = "Ticket title", example = "VPN certificate expired")
        @NotBlank
        @Size(max = 255)
        String title,

        @Schema(description = "Ticket description", example = "VPN client reports certificate expired since this morning.")
        @NotBlank
        @Size(max = 5000)
        String description,

        @Schema(description = "Optional category", example = "VPN")
        @Size(max = 128)
        String category,

        @Schema(description = "Ticket priority")
        TicketPriority priority
) {
}
