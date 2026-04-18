package com.enterprise.ticketing.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Approval command request")
public record ApprovalCommandRequest(
        @Schema(description = "Optional approval comment", example = "Business justification confirmed")
        @Size(max = 2000)
        String comment,
        @Schema(description = "Optional client-generated request id for idempotency", example = "approve-req-001")
        @Size(max = 128)
        String requestId
) {
}
