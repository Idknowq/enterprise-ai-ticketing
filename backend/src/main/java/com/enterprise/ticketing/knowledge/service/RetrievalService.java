package com.enterprise.ticketing.knowledge.service;

import com.enterprise.ticketing.knowledge.dto.RetrievalSearchRequest;
import com.enterprise.ticketing.knowledge.dto.RetrievalSearchResponse;

public interface RetrievalService {

    RetrievalSearchResponse search(RetrievalSearchRequest request);
}
