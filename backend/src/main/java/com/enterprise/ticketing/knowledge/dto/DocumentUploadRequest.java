package com.enterprise.ticketing.knowledge.dto;

import com.enterprise.ticketing.knowledge.domain.KnowledgeAccessLevel;
import com.enterprise.ticketing.knowledge.domain.KnowledgeDocumentCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "Multipart upload form for a knowledge document")
public class DocumentUploadRequest {

    @Schema(description = "Knowledge file", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "file is required")
    private MultipartFile file;

    @Schema(description = "Document title; defaults to filename when omitted")
    private String title;

    @Schema(description = "Standard knowledge document category code", example = "REMOTE_ACCESS")
    @NotNull(message = "category is required")
    private KnowledgeDocumentCategory category;

    @Schema(description = "Owning department; defaults to GLOBAL when omitted", example = "IT")
    private String department;

    @Schema(description = "Access level", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "accessLevel is required")
    private KnowledgeAccessLevel accessLevel;

    @Schema(description = "Document version", example = "v1.0")
    @NotBlank(message = "version is required")
    private String version;

    @Schema(description = "Document update timestamp in ISO-8601 UTC", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "updatedAt is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant updatedAt;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public KnowledgeDocumentCategory getCategory() {
        return category;
    }

    public void setCategory(KnowledgeDocumentCategory category) {
        this.category = category;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public KnowledgeAccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(KnowledgeAccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
