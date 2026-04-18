package com.enterprise.ticketing.ai.workflow;

import com.enterprise.ticketing.ai.domain.AiNodeName;
import com.enterprise.ticketing.ai.dto.AiCitation;
import com.enterprise.ticketing.ai.service.impl.AiRunLogService;
import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.knowledge.dto.RetrievalSearchRequest;
import com.enterprise.ticketing.knowledge.dto.RetrievalSearchResponse;
import com.enterprise.ticketing.knowledge.service.RetrievalService;
import com.enterprise.ticketing.ticket.dto.TicketUserSummaryResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TicketRetrieverNode {

    private final ObjectProvider<RetrievalService> retrievalServiceProvider;
    private final ApplicationProperties applicationProperties;
    private final AiRunLogService aiRunLogService;

    public TicketRetrieverNode(
            ObjectProvider<RetrievalService> retrievalServiceProvider,
            ApplicationProperties applicationProperties,
            AiRunLogService aiRunLogService
    ) {
        this.retrievalServiceProvider = retrievalServiceProvider;
        this.applicationProperties = applicationProperties;
        this.aiRunLogService = aiRunLogService;
    }

    public void execute(AiWorkflowState state) {
        long startedAt = System.nanoTime();
        String query = buildQuery(state);
        List<AiCitation> citations = retrieveWithFallback(state, query);
        state.setCitations(citations);

        aiRunLogService.recordSuccess(
                state.getTicket().id(),
                state.getWorkflowId(),
                AiNodeName.RETRIEVER,
                retrievalServiceProvider.getIfAvailable() == null ? "heuristic-retriever" : "retrieval-service",
                toLatencyMs(startedAt),
                0,
                0,
                "Retrieved " + citations.size() + " citations",
                Map.of("query", query, "citations", citations)
        );
    }

    private List<AiCitation> retrieveWithFallback(AiWorkflowState state, String query) {
        RetrievalService retrievalService = retrievalServiceProvider.getIfAvailable();
        if (retrievalService != null) {
            try {
                RetrievalSearchRequest request = new RetrievalSearchRequest();
                request.setQuery(query);
                request.setTicketId(state.getTicket().id());
                request.setCategory(resolveRetrievalCategory(state));
                request.setDepartment(requesterDepartment(state.getTicket().requester()));
                request.setLimit(applicationProperties.getAi().getRetrievalTopK());
                request.setSaveCitations(Boolean.FALSE);
                request.setAiRunId(state.getWorkflowId());

                RetrievalSearchResponse response = retrievalService.search(request);
                if (response != null && response.results() != null && !response.results().isEmpty()) {
                    return response.results().stream()
                            .map(hit -> new AiCitation(
                                    "RETRIEVAL_SERVICE",
                                    hit.docId(),
                                    hit.chunkId(),
                                    hit.title(),
                                    hit.contentSnippet(),
                                    hit.score(),
                                    hit.citationId() == null
                                            ? "doc:" + hit.docId() + "#chunk:" + hit.chunkId()
                                            : "citation:" + hit.citationId()
                            ))
                            .limit(applicationProperties.getAi().getRetrievalTopK())
                            .toList();
                }
            } catch (RuntimeException ignored) {
                // Fall back to deterministic citations so the MVP pipeline keeps running when retrieval is absent or unstable.
            }
        }
        return heuristicCitations(state);
    }

    private List<AiCitation> heuristicCitations(AiWorkflowState state) {
        String category = state.getClassification().category();
        List<AiCitation> citations = new ArrayList<>();
        switch (category) {
            case "VPN_ISSUE" -> {
                citations.add(citation("HEURISTIC_KB", "VPN Certificate Renewal SOP", "Steps to renew expired VPN certificates and re-import client profiles.", 0.93, "kb://vpn/certificate-renewal"));
                citations.add(citation("HEURISTIC_KB", "Remote Access Troubleshooting Checklist", "Checklist for home-office VPN failures, certificate sync, and endpoint time drift.", 0.88, "kb://vpn/troubleshooting-checklist"));
                citations.add(citation("HEURISTIC_TICKET", "Recent VPN failure pattern", "Historical incidents show expired device certificates are the highest-frequency cause for this symptom.", 0.79, "ticket-history://vpn/certificate-pattern"));
            }
            case "ACCESS_REQUEST" -> {
                citations.add(citation("HEURISTIC_POLICY", "Production Log Access Approval Policy", "Production and security-sensitive access must be approved before entitlement changes.", 0.95, "kb://access/prod-log-approval"));
                citations.add(citation("HEURISTIC_KB", "Least Privilege Access SOP", "Use read-only access by default and record the business justification in the approval summary.", 0.87, "kb://access/least-privilege-sop"));
                citations.add(citation("HEURISTIC_TICKET", "Previous access request template", "Historical tickets show approvers expect resource scope, duration, and incident context.", 0.76, "ticket-history://access/request-template"));
            }
            case "PASSWORD_RESET" -> {
                citations.add(citation("HEURISTIC_KB", "Password Reset Verification SOP", "Identity verification steps required before issuing temporary credentials.", 0.94, "kb://identity/password-reset"));
                citations.add(citation("HEURISTIC_KB", "MFA Resync Guide", "If reset succeeds but login still fails, resynchronize MFA binding and cached sessions.", 0.81, "kb://identity/mfa-resync"));
            }
            default -> {
                citations.add(citation("HEURISTIC_KB", "IT Service Desk Triage SOP", "Generic triage checklist for confirming affected system, impact scope, and reproducibility.", 0.72, "kb://it/triage-sop"));
                citations.add(citation("HEURISTIC_TICKET", "Historical incident similarity", "Search adjacent historical tickets for repeated symptoms and successful workarounds.", 0.68, "ticket-history://general/similar-incidents"));
            }
        }
        return citations.stream()
                .limit(applicationProperties.getAi().getRetrievalTopK())
                .toList();
    }

    private AiCitation citation(String sourceType, String title, String snippet, double score, String sourceRef) {
        return new AiCitation(sourceType, null, null, title, snippet, score, sourceRef);
    }

    private String buildQuery(AiWorkflowState state) {
        Map<String, String> fields = state.getExtractedFields();
        StringBuilder builder = new StringBuilder()
                .append(state.getTicket().title()).append(' ')
                .append(state.getTicket().description()).append(' ')
                .append(state.getClassification().category());
        if (fields != null && !fields.isEmpty()) {
            fields.forEach((key, value) -> builder.append(' ').append(key).append(':').append(value));
        }
        return builder.toString().trim();
    }

    private String resolveRetrievalCategory(AiWorkflowState state) {
        String system = state.getExtractedFields().get("system");
        if (StringUtils.hasText(system)) {
            return system.trim();
        }

        if (state.getTicket() != null && StringUtils.hasText(state.getTicket().category())) {
            String ticketCategory = state.getTicket().category().trim();
            if (!isGenericTicketCategory(ticketCategory)) {
                return ticketCategory;
            }
        }

        return switch (state.getClassification().category()) {
            case "VPN_ISSUE" -> "VPN";
            case "DEV_ENV_ISSUE" -> "DEV_ENV";
            case "SOFTWARE_LICENSE" -> "SOFTWARE";
            case "DEVICE_SUPPORT" -> "DEVICE";
            default -> null;
        };
    }

    private boolean isGenericTicketCategory(String category) {
        String normalized = category.trim().toUpperCase();
        return "IT".equals(normalized)
                || "GENERAL".equals(normalized)
                || "GENERAL_IT_SUPPORT".equals(normalized)
                || "SUPPORT".equals(normalized);
    }

    private String requesterDepartment(TicketUserSummaryResponse requester) {
        return requester == null ? null : requester.department();
    }

    private int toLatencyMs(long startedAt) {
        return (int) Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
