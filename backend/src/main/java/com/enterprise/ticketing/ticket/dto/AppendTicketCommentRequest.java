package com.enterprise.ticketing.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Append comment request")
public record AppendTicketCommentRequest(
        @Schema(description = "Comment content", example = "I retried from another network and got the same error.")
        @NotBlank
        @Size(max = 5000)
        String content
) {
}
