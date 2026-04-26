package com.enterprise.ticketing.ticket.service.impl;

import com.enterprise.ticketing.ai.domain.AiRetrievalStatus;
import com.enterprise.ticketing.ai.dto.AiDecisionResult;
import com.enterprise.ticketing.ai.dto.AiRetrievalDiagnostics;
import com.enterprise.ticketing.ticket.dto.TicketAiDecisionAssessment;
import com.enterprise.ticketing.ticket.service.TicketAiDecisionService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DefaultTicketAiDecisionService implements TicketAiDecisionService {

    @Override
    public TicketAiDecisionAssessment assessDecision(AiDecisionResult decisionResult) {
        List<String> reasons = new ArrayList<>();

        if (decisionResult.needsHumanHandoff()) {
            reasons.add("AI explicitly requested human handoff");
        }

        if (decisionResult.fallbackUsed()) {
            reasons.add("AI provider fallback used" + suffix(decisionResult.fallbackReason()));
        }

        AiRetrievalStatus retrievalStatus = parseRetrievalStatus(decisionResult.retrievalStatus());
        if (retrievalStatus == AiRetrievalStatus.ERROR || retrievalStatus == AiRetrievalStatus.UNAVAILABLE) {
            reasons.add("Retrieval status is " + retrievalStatus + suffix(retrievalMessage(decisionResult.retrievalDiagnostics())));
        }

        boolean manualReviewRequired = !reasons.isEmpty();
        boolean autoFlowAllowed = !manualReviewRequired;
        boolean approvalFlowAllowed = decisionResult.requiresApproval() && autoFlowAllowed;

        String summary;
        if (manualReviewRequired) {
            summary = "Manual review required before auto flow: " + String.join("; ", reasons);
        } else if (approvalFlowAllowed) {
            summary = "AI decision is eligible for automatic approval workflow start";
        } else if (decisionResult.requiresApproval()) {
            summary = "AI decision requires approval but auto flow is blocked";
        } else {
            summary = "AI decision does not require approval and no manual review is currently required";
        }

        return new TicketAiDecisionAssessment(
                autoFlowAllowed,
                manualReviewRequired,
                approvalFlowAllowed,
                summary,
                List.copyOf(reasons)
        );
    }

    private AiRetrievalStatus parseRetrievalStatus(String retrievalStatus) {
        if (!StringUtils.hasText(retrievalStatus)) {
            return AiRetrievalStatus.UNAVAILABLE;
        }
        try {
            return AiRetrievalStatus.valueOf(retrievalStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return AiRetrievalStatus.UNAVAILABLE;
        }
    }

    private String retrievalMessage(AiRetrievalDiagnostics diagnostics) {
        return diagnostics == null ? null : diagnostics.message();
    }

    private String suffix(String text) {
        return StringUtils.hasText(text) ? ": " + text.trim() : "";
    }
}
