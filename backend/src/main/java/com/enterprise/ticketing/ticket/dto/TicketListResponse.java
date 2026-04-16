package com.enterprise.ticketing.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paginated ticket list payload")
public record TicketListResponse(
        List<TicketSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
