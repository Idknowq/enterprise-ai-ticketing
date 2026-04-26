package com.enterprise.ticketing.ai.entity;

import com.enterprise.ticketing.ai.domain.AiNodeName;
import com.enterprise.ticketing.ai.domain.AiRunStatus;
import com.enterprise.ticketing.ticket.entity.TicketEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_runs")
public class AiRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketEntity ticket;

    @Column(name = "workflow_id", nullable = false, length = 64)
    private String workflowId;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_name", nullable = false, length = 64)
    private AiNodeName nodeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AiRunStatus status;

    @Column(name = "provider_type", nullable = false, length = 64)
    private String providerType;

    @Column(name = "model_name", nullable = false, length = 128)
    private String modelName;

    @Column(name = "latency_ms", nullable = false)
    private int latencyMs;

    @Column(name = "token_input", nullable = false)
    private int tokenInput;

    @Column(name = "token_output", nullable = false)
    private int tokenOutput;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode resultPayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "fallback_used", nullable = false)
    private boolean fallbackUsed;

    @Column(name = "fallback_reason", columnDefinition = "TEXT")
    private String fallbackReason;

    @Column(name = "retrieval_status", length = 32)
    private String retrievalStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public TicketEntity getTicket() {
        return ticket;
    }

    public void setTicket(TicketEntity ticket) {
        this.ticket = ticket;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public AiNodeName getNodeName() {
        return nodeName;
    }

    public void setNodeName(AiNodeName nodeName) {
        this.nodeName = nodeName;
    }

    public AiRunStatus getStatus() {
        return status;
    }

    public void setStatus(AiRunStatus status) {
        this.status = status;
    }

    public String getModelName() {
        return modelName;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(int latencyMs) {
        this.latencyMs = latencyMs;
    }

    public int getTokenInput() {
        return tokenInput;
    }

    public void setTokenInput(int tokenInput) {
        this.tokenInput = tokenInput;
    }

    public int getTokenOutput() {
        return tokenOutput;
    }

    public void setTokenOutput(int tokenOutput) {
        this.tokenOutput = tokenOutput;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public JsonNode getResultPayload() {
        return resultPayload;
    }

    public void setResultPayload(JsonNode resultPayload) {
        this.resultPayload = resultPayload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public void setFallbackReason(String fallbackReason) {
        this.fallbackReason = fallbackReason;
    }

    public String getRetrievalStatus() {
        return retrievalStatus;
    }

    public void setRetrievalStatus(String retrievalStatus) {
        this.retrievalStatus = retrievalStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
