package com.enterprise.ticketing.knowledge.dto;

import com.enterprise.ticketing.knowledge.domain.KnowledgeAccessLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Retrieval request for ticket evidence search")
public class RetrievalSearchRequest {

    @Schema(description = "Raw ticket text or retrieval query")
    private String query;

    @Schema(description = "Optional ticket id; when query is omitted the service will build the query from ticket title and description")
    private Long ticketId;

    @Schema(description = "Optional category filter", example = "VPN")
    private String category;

    @Schema(description = "Optional department filter", example = "IT")
    private String department;

    @Schema(description = "Optional access level filter")
    private KnowledgeAccessLevel accessLevel;

    @Schema(description = "Maximum number of evidence items to return", defaultValue = "5")
    @Min(value = 1, message = "limit must be >= 1")
    @Max(value = 10, message = "limit must be <= 10")
    private Integer limit;

    @Schema(description = "Persist citations for later ticket detail or AI run playback", defaultValue = "false")
    private Boolean saveCitations = Boolean.FALSE;

    @Schema(description = "Optional AI run id for downstream traceability")
    private String aiRunId;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
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

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Boolean getSaveCitations() {
        return saveCitations;
    }

    public void setSaveCitations(Boolean saveCitations) {
        this.saveCitations = saveCitations;
    }

    public String getAiRunId() {
        return aiRunId;
    }

    public void setAiRunId(String aiRunId) {
        this.aiRunId = aiRunId;
    }
}
