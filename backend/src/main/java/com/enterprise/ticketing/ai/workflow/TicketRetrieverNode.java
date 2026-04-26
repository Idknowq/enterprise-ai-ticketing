package com.enterprise.ticketing.ai.workflow;

import com.enterprise.ticketing.ai.domain.AiNodeName;
import com.enterprise.ticketing.ai.domain.AiRetrievalStatus;
import com.enterprise.ticketing.ai.dto.AiCitation;
import com.enterprise.ticketing.ai.dto.AiRetrievalDiagnostics;
import com.enterprise.ticketing.ai.service.impl.AiRunLogService;
import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.knowledge.dto.RetrievalSearchRequest;
import com.enterprise.ticketing.knowledge.dto.RetrievalSearchResponse;
import com.enterprise.ticketing.knowledge.service.RetrievalService;
import java.time.Duration;
import java.util.LinkedHashMap;
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
        RetrievalService retrievalService = retrievalServiceProvider.getIfAvailable();
        if (retrievalService == null) {
            state.setRetrievalStatus(AiRetrievalStatus.UNAVAILABLE);
            state.setRetrievalDiagnostics(new AiRetrievalDiagnostics(
                    "UNAVAILABLE",
                    0,
                    0,
                    Map.of("reason", "RetrievalService bean is not available"),
                    "RetrievalService bean is not available"
            ));
            state.setCitations(List.of());
            aiRunLogService.recordFailure(
                    state.getTicket().id(),
                    state.getWorkflowId(),
                    AiNodeName.RETRIEVER,
                    "retrieval",
                    "retrieval-service",
                    toLatencyMs(startedAt),
                    false,
                    null,
                    state.getRetrievalStatus().name(),
                    "RetrievalService bean is not available",
                    Map.of("query", query)
            );
            return;
        }

        RetrievalSearchRequest request = new RetrievalSearchRequest();
        request.setQuery(query);
        request.setTicketId(state.getTicket().id());
        request.setTicketContext(query);
        request.setCategory(resolveRetrievalCategory(state));
        request.setLimit(applicationProperties.getAi().getRetrievalTopK());
        request.setSaveCitations(Boolean.TRUE);
        request.setAiRunId(state.getWorkflowId());

        try {
            RetrievalSearchResponse response = retrievalService.search(request);
            List<AiCitation> citations = response == null || response.results() == null
                    ? List.of()
                    : response.results().stream()
                    .map(hit -> new AiCitation(
                            "RETRIEVAL_SERVICE",
                            hit.docId(),
                            hit.chunkId(),
                            hit.title(),
                            hit.contentSnippet(),
                            hit.score(),
                            hit.retrievalScore(),
                            hit.rerankScore(),
                            resolveSourceRef(hit.docId(), hit.chunkId(), hit.sourceRef(), hit.citationId()),
                            hit.metadataMap()
                    ))
                    .limit(applicationProperties.getAi().getRetrievalTopK())
                    .toList();

            AiRetrievalStatus retrievalStatus = citations.isEmpty() ? AiRetrievalStatus.EMPTY : AiRetrievalStatus.HIT;
            AiRetrievalDiagnostics diagnostics = response == null || response.diagnostics() == null
                    ? new AiRetrievalDiagnostics(
                    "VECTOR_WITH_METADATA_FILTERS",
                    0,
                    citations.size(),
                    Map.of(),
                    citations.isEmpty() ? "Retrieval completed without hits" : "Retrieval completed successfully"
            )
                    : new AiRetrievalDiagnostics(
                    response.diagnostics().retrievalMode(),
                    response.diagnostics().candidateCount(),
                    response.diagnostics().returnedCount(),
                    response.diagnostics().filterSummary(),
                    citations.isEmpty() ? "Retrieval completed without hits" : "Retrieval completed successfully"
            );

            state.setCitations(citations);
            state.setRetrievalStatus(retrievalStatus);
            state.setRetrievalDiagnostics(diagnostics);

            aiRunLogService.recordSuccess(
                    state.getTicket().id(),
                    state.getWorkflowId(),
                    AiNodeName.RETRIEVER,
                    "retrieval",
                    response == null || response.diagnostics() == null ? "retrieval-service" : response.diagnostics().retrievalMode(),
                    toLatencyMs(startedAt),
                    0,
                    0,
                    false,
                    null,
                    retrievalStatus.name(),
                    "Retrieved " + citations.size() + " citations",
                    Map.of(
                            "query", query,
                            "citations", citations,
                            "diagnostics", diagnostics
                    )
            );
        } catch (RuntimeException exception) {
            AiRetrievalDiagnostics diagnostics = new AiRetrievalDiagnostics(
                    "VECTOR_WITH_METADATA_FILTERS",
                    0,
                    0,
                    payload(
                            "category", request.getCategory(),
                            "department", request.getDepartment(),
                            "ticketId", request.getTicketId()
                    ),
                    exception.getMessage()
            );
            state.setCitations(List.of());
            state.setRetrievalStatus(AiRetrievalStatus.ERROR);
            state.setRetrievalDiagnostics(diagnostics);

            aiRunLogService.recordFailure(
                    state.getTicket().id(),
                    state.getWorkflowId(),
                    AiNodeName.RETRIEVER,
                    "retrieval",
                    "retrieval-service",
                    toLatencyMs(startedAt),
                    false,
                    null,
                    state.getRetrievalStatus().name(),
                    exception.getMessage(),
                    payload("query", query, "diagnostics", diagnostics)
            );
        }
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
        Map<String, String> extractedFields = state.getExtractedFields();
        String system = extractedFields == null ? null : extractedFields.get("system");
        if (StringUtils.hasText(system)) {
            return system.trim();
        }

        if (state.getTicket() != null && StringUtils.hasText(state.getTicket().category())) {
            String ticketCategory = state.getTicket().category().trim();
            if (!isGenericTicketCategory(ticketCategory)) {
                return ticketCategory;
            }
        }
        return null;
    }

    private boolean isGenericTicketCategory(String category) {
        String normalized = category.trim().toUpperCase();
        return "IT".equals(normalized)
                || "GENERAL".equals(normalized)
                || "GENERAL_IT_SUPPORT".equals(normalized)
                || "SUPPORT".equals(normalized);
    }

    private String resolveSourceRef(Long documentId, String chunkId, String sourceRef, Long citationId) {
        if (citationId != null) {
            return "citation:" + citationId;
        }
        if (StringUtils.hasText(sourceRef)) {
            return sourceRef;
        }
        return documentId == null || !StringUtils.hasText(chunkId) ? null : "doc:" + documentId + "#chunk:" + chunkId;
    }

    private int toLatencyMs(long startedAt) {
        return (int) Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private Map<String, Object> payload(Object... keyValues) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key instanceof String stringKey && value != null) {
                payload.put(stringKey, value);
            }
        }
        return payload;
    }
}
