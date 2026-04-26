package com.enterprise.ticketing.knowledge.service.impl;

import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.knowledge.domain.KnowledgeAccessLevel;
import com.enterprise.ticketing.knowledge.dto.DocumentMetadataResponse;
import com.enterprise.ticketing.knowledge.dto.RetrievalDiagnosticsResponse;
import com.enterprise.ticketing.knowledge.dto.RetrievalResultItemResponse;
import com.enterprise.ticketing.knowledge.dto.RetrievalSearchRequest;
import com.enterprise.ticketing.knowledge.dto.RetrievalSearchResponse;
import com.enterprise.ticketing.knowledge.qdrant.QdrantClient;
import com.enterprise.ticketing.knowledge.qdrant.QdrantSearchHit;
import com.enterprise.ticketing.knowledge.service.CitationService;
import com.enterprise.ticketing.knowledge.service.EmbeddingProvider;
import com.enterprise.ticketing.knowledge.service.RetrievalService;
import com.enterprise.ticketing.ticket.dto.TicketDetailResponse;
import com.enterprise.ticketing.ticket.service.TicketQueryService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RetrievalServiceImpl implements RetrievalService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{IsHan}]+|[\\p{L}\\p{N}_-]+");

    private final ApplicationProperties applicationProperties;
    private final EmbeddingProvider embeddingProvider;
    private final QdrantClient qdrantClient;
    private final DocumentAccessPolicy documentAccessPolicy;
    private final CitationService citationService;
    private final TicketQueryService ticketQueryService;

    public RetrievalServiceImpl(
            ApplicationProperties applicationProperties,
            EmbeddingProvider embeddingProvider,
            QdrantClient qdrantClient,
            DocumentAccessPolicy documentAccessPolicy,
            CitationService citationService,
            TicketQueryService ticketQueryService
    ) {
        this.applicationProperties = applicationProperties;
        this.embeddingProvider = embeddingProvider;
        this.qdrantClient = qdrantClient;
        this.documentAccessPolicy = documentAccessPolicy;
        this.citationService = citationService;
        this.ticketQueryService = ticketQueryService;
    }

    @Override
    public RetrievalSearchResponse search(RetrievalSearchRequest request) {
        String effectiveQuery = resolveQuery(request);
        if (!StringUtils.hasText(effectiveQuery)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "query or ticketId is required");
        }

        int limit = resolveLimit(request.getLimit());
        Set<KnowledgeAccessLevel> accessLevels = resolveAccessLevels(request.getAccessLevel());
        if (accessLevels.isEmpty()) {
            return new RetrievalSearchResponse(
                    effectiveQuery,
                    request.getTicketId(),
                    new RetrievalDiagnosticsResponse(
                            "VECTOR_WITH_METADATA_FILTERS",
                            0,
                            0,
                            Map.of("reason", "requested access level is not allowed")
                    ),
                    List.of()
            );
        }

        Set<String> departments = documentAccessPolicy.allowedDepartments(request.getDepartment());
        if (departments.contains("__DENY__")) {
            return new RetrievalSearchResponse(
                    effectiveQuery,
                    request.getTicketId(),
                    new RetrievalDiagnosticsResponse(
                            "VECTOR_WITH_METADATA_FILTERS",
                            0,
                            0,
                            Map.of(
                                    "reason", "requested department is not allowed",
                                    "requestedDepartment", request.getDepartment()
                            )
                    ),
                    List.of()
            );
        }

        Map<String, Object> filter = buildFilter(request, accessLevels, departments);
        float[] vector = embeddingProvider.embed(List.of(effectiveQuery)).get(0);
        List<QdrantSearchHit> hits = qdrantClient.search(vector, limit, filter);

        List<RetrievalResultItemResponse> results = hits.stream()
                .limit(limit)
                .map(hit -> toResultItem(hit, effectiveQuery))
                .toList();

        if (Boolean.TRUE.equals(request.getSaveCitations()) && !results.isEmpty()) {
            Map<String, Long> citationIds = citationService.saveCitations(
                    request.getTicketId(),
                    request.getAiRunId(),
                    effectiveQuery,
                    results,
                    documentAccessPolicy.currentUser().getId()
            );
            results = results.stream()
                    .map(result -> {
                        Long citationId = citationIds.get(result.chunkId());
                        return new RetrievalResultItemResponse(
                                result.docId(),
                                result.title(),
                                result.chunkId(),
                                result.contentSnippet(),
                                result.score(),
                                result.retrievalScore(),
                                result.rerankScore(),
                                buildSourceRef(result.docId(), result.chunkId(), citationId),
                                result.metadata(),
                                result.metadataMap(),
                                result.whyMatched(),
                                citationId
                        );
                    })
                    .toList();
        }

        RetrievalDiagnosticsResponse diagnostics = new RetrievalDiagnosticsResponse(
                "VECTOR_WITH_METADATA_FILTERS",
                hits.size(),
                results.size(),
                buildFilterSummary(request, accessLevels, departments)
        );
        return new RetrievalSearchResponse(effectiveQuery, request.getTicketId(), diagnostics, results);
    }

    private String resolveQuery(RetrievalSearchRequest request) {
        if (StringUtils.hasText(request.getQuery())) {
            return request.getQuery().trim();
        }
        if (request.getTicketId() == null) {
            return null;
        }
        TicketDetailResponse ticket = ticketQueryService.getTicketDetail(request.getTicketId());
        List<String> segments = new ArrayList<>();
        if (ticket.ticket() != null) {
            if (StringUtils.hasText(ticket.ticket().title())) {
                segments.add(ticket.ticket().title());
            }
            if (StringUtils.hasText(ticket.ticket().description())) {
                segments.add(ticket.ticket().description());
            }
            if (StringUtils.hasText(ticket.ticket().category())) {
                segments.add(ticket.ticket().category());
            }
        }
        return String.join("\n", segments).trim();
    }

    private int resolveLimit(Integer requestedLimit) {
        int defaultTopK = Math.max(1, applicationProperties.getKnowledge().getDefaultTopK());
        int maxTopK = Math.max(defaultTopK, applicationProperties.getKnowledge().getMaxTopK());
        if (requestedLimit == null) {
            return defaultTopK;
        }
        return Math.min(Math.max(1, requestedLimit), maxTopK);
    }

    private Set<KnowledgeAccessLevel> resolveAccessLevels(KnowledgeAccessLevel requested) {
        Set<KnowledgeAccessLevel> allowed = documentAccessPolicy.allowedAccessLevels();
        if (requested == null) {
            return allowed;
        }
        return allowed.contains(requested) ? Set.of(requested) : Set.of();
    }

    private Map<String, Object> buildFilter(
            RetrievalSearchRequest request,
            Set<KnowledgeAccessLevel> accessLevels,
            Set<String> departments
    ) {
        List<Map<String, Object>> must = new ArrayList<>();
        if (request.getCategory() != null) {
            must.add(qdrantClient.matchFilter("category", request.getCategory().code()));
        }
        if (!departments.isEmpty()) {
            must.add(qdrantClient.anyFilter("department", departments));
        }
        must.add(qdrantClient.anyFilter(
                "accessLevel",
                accessLevels.stream().map(Enum::name).collect(LinkedHashSet::new, Set::add, Set::addAll)
        ));
        return must.isEmpty() ? Map.of() : Map.of("must", must);
    }

    private RetrievalResultItemResponse toResultItem(QdrantSearchHit hit, String query) {
        Map<String, Object> payload = hit.payload();
        Long documentId = longValue(payload.get("docId"));
        String title = stringValue(payload.get("title"));
        String chunkId = stringValue(payload.get("chunkId"));
        String snippet = stringValue(payload.get("contentSnippet"));
        String category = stringValue(payload.get("category"));
        String department = stringValue(payload.get("department"));
        KnowledgeAccessLevel accessLevel = KnowledgeAccessLevel.valueOf(stringValue(payload.get("accessLevel")));
        String version = stringValue(payload.get("version"));
        Instant updatedAt = Instant.parse(stringValue(payload.get("updatedAt")));

        DocumentMetadataResponse metadata = new DocumentMetadataResponse(
                documentId,
                title,
                category,
                department,
                accessLevel,
                version,
                updatedAt
        );

        return new RetrievalResultItemResponse(
                documentId,
                title,
                chunkId,
                snippet,
                hit.score(),
                hit.score(),
                null,
                buildSourceRef(documentId, chunkId, null),
                metadata,
                buildMetadataMap(metadata),
                buildWhyMatched(query, snippet, category),
                null
        );
    }

    private Map<String, Object> buildFilterSummary(
            RetrievalSearchRequest request,
            Set<KnowledgeAccessLevel> accessLevels,
            Set<String> departments
    ) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("category", request.getCategory() == null ? null : request.getCategory().code());
        summary.put("departments", departments);
        summary.put("accessLevels", accessLevels.stream().map(Enum::name).toList());
        summary.put("ticketContextProvided", StringUtils.hasText(request.getTicketContext()));
        return summary;
    }

    private Map<String, Object> buildMetadataMap(DocumentMetadataResponse metadata) {
        return Map.of(
                "docId", metadata.docId(),
                "title", metadata.title(),
                "category", metadata.category(),
                "department", metadata.department(),
                "accessLevel", metadata.accessLevel().name(),
                "version", metadata.version(),
                "updatedAt", metadata.updatedAt().toString()
        );
    }

    private String buildSourceRef(Long documentId, String chunkId, Long citationId) {
        if (citationId != null) {
            return "citation:" + citationId;
        }
        if (documentId == null || !StringUtils.hasText(chunkId)) {
            return null;
        }
        return "doc:" + documentId + "#chunk:" + chunkId;
    }

    private String buildWhyMatched(String query, String snippet, String category) {
        Set<String> matchedTerms = new LinkedHashSet<>();
        String normalizedSnippet = snippet == null ? "" : snippet.toLowerCase(Locale.ROOT);
        Matcher matcher = TOKEN_PATTERN.matcher(query.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() > 1 && normalizedSnippet.contains(token)) {
                matchedTerms.add(token);
            }
            if (matchedTerms.size() >= 3) {
                break;
            }
        }

        if (!matchedTerms.isEmpty()) {
            return "Matched keywords: " + String.join(", ", matchedTerms);
        }
        if (StringUtils.hasText(category)) {
            return "Matched metadata category: " + category;
        }
        return "Matched semantic evidence from knowledge base";
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.parseLong(value.toString());
    }
}
