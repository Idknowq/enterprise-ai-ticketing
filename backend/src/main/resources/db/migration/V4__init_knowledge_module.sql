CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    source_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    document_type VARCHAR(32) NOT NULL,
    category VARCHAR(128) NOT NULL,
    department VARCHAR(64) NOT NULL,
    access_level VARCHAR(32) NOT NULL,
    version VARCHAR(64) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    content_text TEXT NOT NULL,
    chunk_count INT NOT NULL DEFAULT 0,
    index_status VARCHAR(32) NOT NULL,
    last_indexed_at TIMESTAMPTZ,
    embedding_model VARCHAR(128) NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_documents_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_id VARCHAR(128) NOT NULL,
    chunk_index INT NOT NULL,
    vector_point_id VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    content_snippet VARCHAR(512) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_document_chunks_document FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT uk_document_chunks_chunk_id UNIQUE (chunk_id),
    CONSTRAINT uk_document_chunks_vector_point_id UNIQUE (vector_point_id),
    CONSTRAINT uk_document_chunks_document_index UNIQUE (document_id, chunk_index)
);

CREATE TABLE IF NOT EXISTS citations (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT,
    ai_run_id VARCHAR(128),
    document_id BIGINT NOT NULL,
    chunk_id VARCHAR(128) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content_snippet TEXT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    category VARCHAR(128) NOT NULL,
    department VARCHAR(64) NOT NULL,
    access_level VARCHAR(32) NOT NULL,
    version VARCHAR(64) NOT NULL,
    document_updated_at TIMESTAMPTZ NOT NULL,
    search_query TEXT NOT NULL,
    why_matched VARCHAR(512),
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_citations_document FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_citations_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_documents_category ON documents (category);
CREATE INDEX IF NOT EXISTS idx_documents_department ON documents (department);
CREATE INDEX IF NOT EXISTS idx_documents_access_level ON documents (access_level);
CREATE INDEX IF NOT EXISTS idx_documents_index_status ON documents (index_status);
CREATE INDEX IF NOT EXISTS idx_documents_updated_at ON documents (updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id ON document_chunks (document_id);
CREATE INDEX IF NOT EXISTS idx_citations_ticket_id ON citations (ticket_id);
CREATE INDEX IF NOT EXISTS idx_citations_ai_run_id ON citations (ai_run_id);
CREATE INDEX IF NOT EXISTS idx_citations_document_id ON citations (document_id);
CREATE INDEX IF NOT EXISTS idx_citations_created_at ON citations (created_at DESC);
