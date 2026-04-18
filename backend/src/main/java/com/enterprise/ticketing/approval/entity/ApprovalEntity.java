package com.enterprise.ticketing.approval.entity;

import com.enterprise.ticketing.approval.domain.ApprovalStageKey;
import com.enterprise.ticketing.approval.domain.ApprovalStatus;
import com.enterprise.ticketing.auth.entity.UserEntity;
import com.enterprise.ticketing.ticket.entity.TicketEntity;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "approvals")
public class ApprovalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketEntity ticket;

    @Column(name = "workflow_id", nullable = false, length = 128)
    private String workflowId;

    @Column(name = "ai_workflow_id", length = 64)
    private String aiWorkflowId;

    @Column(name = "stage_order", nullable = false)
    private int stageOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_key", nullable = false, length = 64)
    private ApprovalStageKey stageKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_id", nullable = false)
    private UserEntity approver;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "decision_request_id", length = 128)
    private String decisionRequestId;

    @Column(name = "decision_trace_id", length = 64)
    private String decisionTraceId;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

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

    public String getAiWorkflowId() {
        return aiWorkflowId;
    }

    public void setAiWorkflowId(String aiWorkflowId) {
        this.aiWorkflowId = aiWorkflowId;
    }

    public int getStageOrder() {
        return stageOrder;
    }

    public void setStageOrder(int stageOrder) {
        this.stageOrder = stageOrder;
    }

    public ApprovalStageKey getStageKey() {
        return stageKey;
    }

    public void setStageKey(ApprovalStageKey stageKey) {
        this.stageKey = stageKey;
    }

    public UserEntity getApprover() {
        return approver;
    }

    public void setApprover(UserEntity approver) {
        this.approver = approver;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDecisionRequestId() {
        return decisionRequestId;
    }

    public void setDecisionRequestId(String decisionRequestId) {
        this.decisionRequestId = decisionRequestId;
    }

    public String getDecisionTraceId() {
        return decisionTraceId;
    }

    public void setDecisionTraceId(String decisionTraceId) {
        this.decisionTraceId = decisionTraceId;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (requestedAt == null) {
            requestedAt = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
