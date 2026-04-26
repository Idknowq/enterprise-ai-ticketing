package com.enterprise.ticketing.knowledge.service.impl;

import com.enterprise.ticketing.auth.security.UserPrincipal;
import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.knowledge.domain.KnowledgeAccessLevel;
import com.enterprise.ticketing.knowledge.domain.KnowledgeDocumentCategory;
import com.enterprise.ticketing.knowledge.domain.KnowledgeDocumentType;
import com.enterprise.ticketing.knowledge.dto.DocumentUploadRequest;
import com.enterprise.ticketing.knowledge.entity.DocumentEntity;
import com.enterprise.ticketing.knowledge.parser.DocumentParserRegistry;
import com.enterprise.ticketing.knowledge.parser.ParsedDocument;
import com.enterprise.ticketing.knowledge.qdrant.QdrantClient;
import com.enterprise.ticketing.knowledge.qdrant.QdrantPoint;
import com.enterprise.ticketing.knowledge.repository.DocumentChunkRepository;
import com.enterprise.ticketing.knowledge.repository.DocumentRepository;
import com.enterprise.ticketing.knowledge.service.EmbeddingProvider;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceImplTest {

    @Test
    void uploadWritesStandardCategoryCodeToDocumentAndQdrantPayload() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        DocumentParserRegistry parserRegistry = mock(DocumentParserRegistry.class);
        EmbeddingProvider embeddingProvider = mock(EmbeddingProvider.class);
        QdrantClient qdrantClient = mock(QdrantClient.class);
        DocumentAccessPolicy accessPolicy = mock(DocumentAccessPolicy.class);

        when(parserRegistry.parse(any())).thenReturn(new ParsedDocument(
                KnowledgeDocumentType.TXT,
                "VPN certificate expired troubleshooting steps",
                "text/plain"
        ));
        when(embeddingProvider.modelName()).thenReturn("nomic-embed-text:latest");
        when(embeddingProvider.embed(any())).thenReturn(List.of(new float[]{0.1F, 0.2F}));
        when(accessPolicy.normalizeDepartment("IT")).thenReturn("IT");
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getId()).thenReturn(1L);
        when(accessPolicy.currentUser()).thenReturn(principal);
        when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(invocation -> {
            DocumentEntity document = invocation.getArgument(0);
            if (document.getId() == null) {
                ReflectionTestUtils.setField(document, "id", 42L);
            }
            return document;
        });

        ApplicationProperties properties = new ApplicationProperties();
        properties.getKnowledge().setChunkSize(200);
        DocumentServiceImpl service = new DocumentServiceImpl(
                documentRepository,
                documentChunkRepository,
                parserRegistry,
                new TextChunker(properties),
                embeddingProvider,
                qdrantClient,
                accessPolicy
        );

        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setFile(new MockMultipartFile("file", "vpn.txt", "text/plain", "content".getBytes()));
        request.setTitle("VPN SOP");
        request.setCategory(KnowledgeDocumentCategory.REMOTE_ACCESS);
        request.setDepartment("IT");
        request.setAccessLevel(KnowledgeAccessLevel.INTERNAL);
        request.setVersion("v1.0");
        request.setUpdatedAt(Instant.parse("2026-04-26T10:00:00Z"));

        service.uploadDocument(request);

        ArgumentCaptor<List<QdrantPoint>> pointsCaptor = forClass(List.class);
        verify(qdrantClient).upsert(pointsCaptor.capture());
        assertThat(pointsCaptor.getValue()).hasSize(1);
        assertThat(pointsCaptor.getValue().get(0).payload()).containsEntry("category", "REMOTE_ACCESS");
    }
}
