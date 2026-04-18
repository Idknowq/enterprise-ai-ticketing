CREATE TABLE IF NOT EXISTS ai_runs (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    workflow_id VARCHAR(64) NOT NULL,
    node_name VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    latency_ms INT NOT NULL,
    token_input INT NOT NULL DEFAULT 0,
    token_output INT NOT NULL DEFAULT 0,
    result_summary TEXT,
    result_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_runs_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_runs_ticket_created_at ON ai_runs (ticket_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_runs_workflow_id ON ai_runs (workflow_id);
CREATE INDEX IF NOT EXISTS idx_ai_runs_ticket_workflow_created_at ON ai_runs (ticket_id, workflow_id, created_at ASC);
