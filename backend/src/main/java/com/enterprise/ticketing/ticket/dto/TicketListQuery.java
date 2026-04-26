package com.enterprise.ticketing.ticket.dto;

import com.enterprise.ticketing.knowledge.domain.KnowledgeDocumentCategory;
import com.enterprise.ticketing.ticket.domain.TicketPriority;
import com.enterprise.ticketing.ticket.domain.TicketStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class TicketListQuery {

    private String keyword;
    private TicketStatus status;
    private TicketPriority priority;
    private KnowledgeDocumentCategory category;
    private Long requesterId;
    private Long assigneeId;

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 20;

    private String sortBy = "updatedAt";
    private String sortDirection = "DESC";

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public KnowledgeDocumentCategory getCategory() {
        return category;
    }

    public void setCategory(KnowledgeDocumentCategory category) {
        this.category = category;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
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

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
