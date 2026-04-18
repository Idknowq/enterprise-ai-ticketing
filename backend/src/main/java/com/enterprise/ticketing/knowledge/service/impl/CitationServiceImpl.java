package com.enterprise.ticketing.knowledge.service.impl;

import com.enterprise.ticketing.knowledge.dto.DocumentMetadataResponse;
import com.enterprise.ticketing.knowledge.dto.RetrievalResultItemResponse;
import com.enterprise.ticketing.knowledge.entity.CitationEntity;
import com.enterprise.ticketing.knowledge.repository.CitationRepository;
import com.enterprise.ticketing.knowledge.service.CitationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CitationServiceImpl implements CitationService {

    private final CitationRepository citationRepository;

    public CitationServiceImpl(CitationRepository citationRepository) {
        this.citationRepository = citationRepository;
    }

    @Override
    @Transactional
    public Map<String, Long> saveCitations(
            Long ticketId,
            String aiRunId,
            String searchQuery,
            List<RetrievalResultItemResponse> results,
            Long createdByUserId
    ) {
        Map<String, Long> citationIds = new LinkedHashMap<>();
        List<CitationEntity> entities = results.stream()
                .map(result -> toEntity(ticketId, aiRunId, searchQuery, result, createdByUserId))
                .toList();

        List<CitationEntity> saved = citationRepository.saveAll(entities);
        for (CitationEntity entity : saved) {
            citationIds.put(entity.getChunkId(), entity.getId());
        }
        return citationIds;
    }

    private CitationEntity toEntity(
            Long ticketId,
            String aiRunId,
            String searchQuery,
            RetrievalResultItemResponse result,
            Long createdByUserId
    ) {
        DocumentMetadataResponse metadata = result.metadata();
        CitationEntity entity = new CitationEntity();
        entity.setTicketId(ticketId);
        entity.setAiRunId(aiRunId);
        entity.setDocumentId(result.docId());
        entity.setChunkId(result.chunkId());
        entity.setTitle(result.title());
        entity.setContentSnippet(result.contentSnippet());
        entity.setScore(result.score());
        entity.setCategory(metadata.category());
        entity.setDepartment(metadata.department());
        entity.setAccessLevel(metadata.accessLevel());
        entity.setVersion(metadata.version());
        entity.setDocumentUpdatedAt(metadata.updatedAt());
        entity.setSearchQuery(searchQuery);
        entity.setWhyMatched(result.whyMatched());
        entity.setCreatedByUserId(createdByUserId);
        return entity;
    }
}
