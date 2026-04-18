package com.enterprise.ticketing.knowledge.service;

import com.enterprise.ticketing.knowledge.dto.DocumentListQuery;
import com.enterprise.ticketing.knowledge.dto.DocumentListResponse;
import com.enterprise.ticketing.knowledge.dto.DocumentResponse;
import com.enterprise.ticketing.knowledge.dto.DocumentUploadRequest;

public interface DocumentService {

    DocumentResponse uploadDocument(DocumentUploadRequest request);

    DocumentListResponse listDocuments(DocumentListQuery query);
}
