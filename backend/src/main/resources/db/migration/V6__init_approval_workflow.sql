CREATE TABLE approvals (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    workflow_id VARCHAR(128) NOT NULL,
    ai_workflow_id VARCHAR(64),
    stage_order INT NOT NULL,
    stage_key VARCHAR(64) NOT NULL,
    approver_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(32) NOT NULL,
    comment TEXT,
    decision_request_id VARCHAR(128),
    decision_trace_id VARCHAR(64),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_approvals_workflow_stage
    ON approvals(workflow_id, stage_order);

CREATE INDEX idx_approvals_ticket_id
    ON approvals(ticket_id);

CREATE INDEX idx_approvals_workflow_id
    ON approvals(workflow_id);

CREATE INDEX idx_approvals_approver_status
    ON approvals(approver_id, status, requested_at);

CREATE INDEX idx_approvals_status
    ON approvals(status, requested_at);
