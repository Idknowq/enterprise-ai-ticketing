package com.enterprise.ticketing.ai.provider;

import com.enterprise.ticketing.ticket.domain.TicketPriority;

public record AiClassificationInput(
        String title,
        String description,
        String existingCategory,
        TicketPriority existingPriority
) {
}
