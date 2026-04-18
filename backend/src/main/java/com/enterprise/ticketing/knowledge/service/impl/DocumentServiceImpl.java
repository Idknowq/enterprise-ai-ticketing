package com.enterprise.ticketing.knowledge.service.impl;

import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.knowledge.domain.DocumentIndexStatus;
import com.enterprise.ticketing.knowledge.domain.KnowledgeAccessLevel;
import com.enterprise.ticketing.knowledge.dto.DocumentListQuery;
import com.enterprise.ticketing.knowledge.dto.DocumentListResponse;
import com.enterprise.ticketing.knowledge.dto.DocumentMetadataResponse;
import com.enterprise.ticketing.knowledge.dto.DocumentResponse;
import com.enterprise.ticketing.knowledge.dto.DocumentUploadRequest;
import com.enterprise.ticketing.knowledge.entity.DocumentChunkEntity;
import com.enterprise.ticketing.knowledge.entity.DocumentEntity;
import com.enterprise.ticketing.knowledge.parser.DocumentParserRegistry;
import com.enterprise.ticketing.knowledge.parser.ParsedDocument;
import com.enterprise.ticketing.knowledge.qdrant.QdrantClient;
import com.enterprise.ticketing.knowledge.qdrant.QdrantPoint;
import com.enterprise.ticketing.knowledge.repository.DocumentChunkRepository;
import com.enterprise.ticketing.knowledge.repository.DocumentRepository;
import com.enterprise.ticketing.knowledge.service.DocumentService;
import com.enterprise.ticketing.knowledge.service.EmbeddingProvider;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentParserRegistry documentParserRegistry;
    private final TextChunker textChunker;
    private final EmbeddingProvider embeddingProvider;
    private final QdrantClient qdrantClient;
    private final DocumentAccessPolicy documentAccessPolicy;

    public DocumentServiceImpl(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            DocumentParserRegistry documentParserRegistry,
            TextChunker textChunker,
            EmbeddingProvider embeddingProvider,
            QdrantClient qdrantClient,
            DocumentAccessPolicy documentAccessPolicy
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.documentParserRegistry = documentParserRegistry;
        this.textChunker = textChunker;
        this.embeddingProvider = embeddingProvider;
        this.qdrantClient = qdrantClient;
        this.documentAccessPolicy = documentAccessPolicy;
    }

    @Override
    public DocumentResponse uploadDocument(DocumentUploadRequest request) {
        documentAccessPolicy.checkDocumentManagementAccess();
        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "Document file cannot be empty");
        }

        ParsedDocument parsedDocument = documentParserRegistry.parse(file);
        if (!StringUtils.hasText(parsedDocument.content())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_PROCESSING_FAILED, "Parsed document content is empty");
        }

        DocumentEntity document = new DocumentEntity();
        document.setTitle(resolveTitle(request.getTitle(), file));
        document.setSourceFilename(resolveFilename(file));
        document.setContentType(parsedDocument.contentType());
        document.setDocumentType(parsedDocument.documentType());
        document.setCategory(documentAccessPolicy.normalizeCategory(request.getCategory()));
        document.setDepartment(documentAccessPolicy.normalizeDepartment(request.getDepartment()));
        document.setAccessLevel(request.getAccessLevel());
        document.setVersion(request.getVersion().trim());
        document.setUpdatedAt(request.getUpdatedAt());
        document.setContentText(parsedDocument.content());
        document.setChunkCount(0);
        document.setIndexStatus(DocumentIndexStatus.PENDING);
        document.setEmbeddingModel(embeddingProvider.modelName());
        document.setCreatedByUserId(documentAccessPolicy.currentUser().getId());
        document = documentRepository.save(document);

        try {
            List<TextChunk> chunks = textChunker.chunk(parsedDocument.content());
            if (chunks.isEmpty()) {
                throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_PROCESSING_FAILED, "No chunks generated from document");
            }

            List<float[]> embeddings = embeddingProvider.embed(chunks.stream().map(TextChunk::content).toList());
            List<QdrantPoint> points = new ArrayList<>(chunks.size());
            List<DocumentChunkEntity> chunkEntities = new ArrayList<>(chunks.size());

            for (int i = 0; i < chunks.size(); i++) {
                TextChunk chunk = chunks.get(i);
                String chunkId = buildChunkId(document.getId(), chunk.index());
                String vectorPointId = buildVectorPointId();
                chunkEntities.add(toChunkEntity(document, chunkId, vectorPointId, chunk));
                points.add(new QdrantPoint(
                        vectorPointId,
                        embeddings.get(i),
                        buildPayload(document, chunkId, chunk)
                ));
            }

            qdrantClient.upsert(points);
            documentChunkRepository.saveAll(chunkEntities);

            document.setChunkCount(chunkEntities.size());
            document.setIndexStatus(DocumentIndexStatus.INDEXED);
            document.setLastIndexedAt(Instant.now());
            return toResponse(documentRepository.save(document));
        } catch (BusinessException exception) {
            markFailed(document);
            throw exception;
        } catch (Exception exception) {
            markFailed(document);
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_PROCESSING_FAILED, "Failed to index knowledge document");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentListResponse listDocuments(DocumentListQuery query) {
        documentAccessPolicy.checkDocumentManagementAccess();
        Set<KnowledgeAccessLevel> allowedAccessLevels = resolveAllowedAccessLevels(query.getAccessLevel());
        if (allowedAccessLevels.isEmpty()) {
            return new DocumentListResponse(List.of(), query.getPage(), query.getSize(), 0, 0);
        }

        Set<String> departments = documentAccessPolicy.allowedDepartments(query.getDepartment());
        if (departments.contains("__DENY__")) {
            return new DocumentListResponse(List.of(), query.getPage(), query.getSize(), 0, 0);
        }

        Page<DocumentEntity> page = documentRepository.findAll(
                buildSpecification(query, allowedAccessLevels, departments),
                PageRequest.of(query.getPage(), query.getSize(), Sort.by(Sort.Direction.DESC, "updatedAt"))
        );

        return new DocumentListResponse(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private Specification<DocumentEntity> buildSpecification(
            DocumentListQuery query,
            Set<KnowledgeAccessLevel> allowedAccessLevels,
            Set<String> departments
    ) {
        return Specification.where(keywordSpecification(query.getKeyword()))
                .and(exactStringSpecification("category", documentAccessPolicy.normalizeCategory(query.getCategory())))
                .and(accessLevelSpecification(allowedAccessLevels))
                .and(departmentSpecification(departments))
                .and(exactSpecification("indexStatus", query.getIndexStatus()));
    }

    private Specification<DocumentEntity> keywordSpecification(String keyword) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }
            String likePattern = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("sourceFilename")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("contentText")), likePattern)
            );
        };
    }

    private Specification<DocumentEntity> exactStringSpecification(String field, String value) {
        return (root, criteriaQuery, criteriaBuilder) -> !StringUtils.hasText(value)
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get(field), value);
    }

    private Specification<DocumentEntity> exactSpecification(String field, Object value) {
        return (root, criteriaQuery, criteriaBuilder) -> value == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get(field), value);
    }

    private Specification<DocumentEntity> accessLevelSpecification(Set<KnowledgeAccessLevel> allowedAccessLevels) {
        return (root, criteriaQuery, criteriaBuilder) -> root.get("accessLevel").in(allowedAccessLevels);
    }

    private Specification<DocumentEntity> departmentSpecification(Set<String> departments) {
        return (root, criteriaQuery, criteriaBuilder) -> departments.isEmpty()
                ? criteriaBuilder.conjunction()
                : root.get("department").in(departments);
    }

    private Set<KnowledgeAccessLevel> resolveAllowedAccessLevels(KnowledgeAccessLevel requestedAccessLevel) {
        Set<KnowledgeAccessLevel> allowed = documentAccessPolicy.allowedAccessLevels();
        if (requestedAccessLevel == null) {
            return allowed;
        }
        return allowed.contains(requestedAccessLevel) ? Set.of(requestedAccessLevel) : Set.of();
    }

    private DocumentChunkEntity toChunkEntity(
            DocumentEntity document,
            String chunkId,
            String vectorPointId,
            TextChunk chunk
    ) {
        DocumentChunkEntity entity = new DocumentChunkEntity();
        entity.setDocument(document);
        entity.setChunkId(chunkId);
        entity.setChunkIndex(chunk.index());
        entity.setVectorPointId(vectorPointId);
        entity.setContent(chunk.content());
        entity.setContentSnippet(chunk.snippet());
        return entity;
    }

    private Map<String, Object> buildPayload(DocumentEntity document, String chunkId, TextChunk chunk) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("docId", document.getId());
        payload.put("title", document.getTitle());
        payload.put("chunkId", chunkId);
        payload.put("chunkIndex", chunk.index());
        payload.put("contentSnippet", chunk.snippet());
        payload.put("category", document.getCategory());
        payload.put("department", document.getDepartment());
        payload.put("accessLevel", document.getAccessLevel().name());
        payload.put("version", document.getVersion());
        payload.put("updatedAt", document.getUpdatedAt().toString());
        return payload;
    }

    private String resolveTitle(String requestedTitle, MultipartFile file) {
        if (StringUtils.hasText(requestedTitle)) {
            return requestedTitle.trim();
        }
        String filename = resolveFilename(file);
        int extensionIndex = filename.lastIndexOf('.');
        return extensionIndex > 0 ? filename.substring(0, extensionIndex) : filename;
    }

    private String resolveFilename(MultipartFile file) {
        return StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : "knowledge-document";
    }

    private String buildChunkId(Long documentId, int chunkIndex) {
        return "doc-" + documentId + "-chunk-" + chunkIndex;
    }

    private String buildVectorPointId() {
        return UUID.randomUUID().toString();
    }

    private void markFailed(DocumentEntity document) {
        document.setIndexStatus(DocumentIndexStatus.FAILED);
        documentRepository.save(document);
    }

    private DocumentResponse toResponse(DocumentEntity document) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getSourceFilename(),
                document.getDocumentType(),
                document.getIndexStatus(),
                document.getChunkCount(),
                document.getEmbeddingModel(),
                new DocumentMetadataResponse(
                        document.getId(),
                        document.getTitle(),
                        document.getCategory(),
                        document.getDepartment(),
                        document.getAccessLevel(),
                        document.getVersion(),
                        document.getUpdatedAt()
                ),
                document.getLastIndexedAt(),
                document.getCreatedAt()
        );
    }
}
