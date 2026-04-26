package com.enterprise.ticketing.ticket.dto;

import java.util.List;

public record TicketAiDecisionAssessment(
        boolean autoFlowAllowed,
        boolean manualReviewRequired,
        boolean approvalFlowAllowed,
        String summary,
        List<String> reasons
) {
}
