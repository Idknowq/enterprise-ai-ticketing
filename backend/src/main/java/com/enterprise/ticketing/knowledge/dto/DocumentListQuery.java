package com.enterprise.ticketing.knowledge.dto;

import com.enterprise.ticketing.knowledge.domain.DocumentIndexStatus;
import com.enterprise.ticketing.knowledge.domain.KnowledgeAccessLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Knowledge document list query")
public class DocumentListQuery {

    private String keyword;
    private String category;
    private String department;
    private KnowledgeAccessLevel accessLevel;
    private DocumentIndexStatus indexStatus;

    @Min(value = 0, message = "page must be >= 0")
    private int page = 0;

    @Min(value = 1, message = "size must be >= 1")
    @Max(value = 100, message = "size must be <= 100")
    private int size = 20;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
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

    public DocumentIndexStatus getIndexStatus() {
        return indexStatus;
    }

    public void setIndexStatus(DocumentIndexStatus indexStatus) {
        this.indexStatus = indexStatus;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
