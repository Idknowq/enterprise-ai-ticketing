package com.enterprise.ticketing.observability.dto;

import java.util.Map;

public record DashboardMetricsResponse(
        long totalTickets,
        Map<String, Long> ticketStatusDistribution,
        double aiSuggestionSuccessRate,
        double averageAiLatencyMs,
        double averageRetrievalLatencyMs,
        double averageApprovalWaitMs,
        long pendingApprovals,
        double aiManualReviewRequiredCount,
        double workflowFailureCount,
        double workflowRetryCount
) {
}
