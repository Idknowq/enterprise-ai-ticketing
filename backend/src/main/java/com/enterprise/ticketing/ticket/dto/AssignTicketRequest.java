package com.enterprise.ticketing.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Assign ticket request")
public record AssignTicketRequest(
        @Schema(description = "Assignee user id", example = "2")
        @NotNull
        Long assigneeId,

        @Schema(description = "Optional assignment note", example = "Transferred to VPN support queue")
        @Size(max = 255)
        String note
) {
}
