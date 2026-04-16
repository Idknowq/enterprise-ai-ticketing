package com.enterprise.ticketing.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Aggregated ticket detail with comments and timeline")
public record TicketDetailResponse(
        TicketResponse ticket,
        List<TicketCommentResponse> comments,
        List<TicketEventResponse> timeline
) {
}
