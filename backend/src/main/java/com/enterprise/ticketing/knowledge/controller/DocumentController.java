package com.enterprise.ticketing.knowledge.controller;

import com.enterprise.ticketing.common.api.OpenApiResultSchemas;
import com.enterprise.ticketing.common.api.Result;
import com.enterprise.ticketing.common.util.TraceIdUtils;
import com.enterprise.ticketing.knowledge.dto.DocumentListQuery;
import com.enterprise.ticketing.knowledge.dto.DocumentListResponse;
import com.enterprise.ticketing.knowledge.dto.DocumentResponse;
import com.enterprise.ticketing.knowledge.dto.DocumentUploadRequest;
import com.enterprise.ticketing.knowledge.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Knowledge Documents", description = "Knowledge document ingestion and management APIs")
@Validated
@RestController
@RequestMapping("${app.api-base-path:/api}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "Upload document", description = "Upload, parse, chunk, embed, and index a knowledge document.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@roleChecker.hasRole(authentication, 'ADMIN')")
    public Result<DocumentResponse> uploadDocument(@Valid @ModelAttribute DocumentUploadRequest request) {
        return Result.success(documentService.uploadDocument(request), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "List documents", description = "List indexed knowledge documents with metadata filters.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OpenApiResultSchemas.DocumentListResult.class)))
    @GetMapping
    @PreAuthorize("@roleChecker.hasAnyRole(authentication, 'ADMIN', 'SUPPORT_AGENT')")
    public Result<DocumentListResponse> listDocuments(@Valid @ModelAttribute DocumentListQuery query) {
        return Result.success(documentService.listDocuments(query), TraceIdUtils.currentTraceId());
    }
}
