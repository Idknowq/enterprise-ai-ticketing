package com.enterprise.ticketing.knowledge.service;

import com.enterprise.ticketing.knowledge.dto.RetrievalResultItemResponse;
import java.util.List;
import java.util.Map;

public interface CitationService {

    Map<String, Long> saveCitations(
            Long ticketId,
            String aiRunId,
            String searchQuery,
            List<RetrievalResultItemResponse> results,
            Long createdByUserId
    );
}
