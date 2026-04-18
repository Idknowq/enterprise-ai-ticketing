package com.enterprise.ticketing.ai.provider;

import com.enterprise.ticketing.ticket.domain.TicketPriority;

public record AiClassificationOutput(
        String category,
        TicketPriority priority,
        double confidence
) {
}
