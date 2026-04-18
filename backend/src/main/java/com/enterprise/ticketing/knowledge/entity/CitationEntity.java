package com.enterprise.ticketing.knowledge.entity;

import com.enterprise.ticketing.knowledge.domain.KnowledgeAccessLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "citations")
public class CitationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "ai_run_id", length = 128)
    private String aiRunId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "chunk_id", nullable = false, length = 128)
    private String chunkId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content_snippet", nullable = false, columnDefinition = "TEXT")
    private String contentSnippet;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "category", nullable = false, length = 128)
    private String category;

    @Column(name = "department", nullable = false, length = 64)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 32)
    private KnowledgeAccessLevel accessLevel;

    @Column(name = "version", nullable = false, length = 64)
    private String version;

    @Column(name = "document_updated_at", nullable = false)
    private Instant documentUpdatedAt;

    @Column(name = "search_query", nullable = false, columnDefinition = "TEXT")
    private String searchQuery;

    @Column(name = "why_matched", length = 512)
    private String whyMatched;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getAiRunId() {
        return aiRunId;
    }

    public void setAiRunId(String aiRunId) {
        this.aiRunId = aiRunId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentSnippet() {
        return contentSnippet;
    }

    public void setContentSnippet(String contentSnippet) {
        this.contentSnippet = contentSnippet;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Instant getDocumentUpdatedAt() {
        return documentUpdatedAt;
    }

    public void setDocumentUpdatedAt(Instant documentUpdatedAt) {
        this.documentUpdatedAt = documentUpdatedAt;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getWhyMatched() {
        return whyMatched;
    }

    public void setWhyMatched(String whyMatched) {
        this.whyMatched = whyMatched;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
