CREATE TABLE document_versions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id       UUID NOT NULL REFERENCES documents(id),
    version_number    INT NOT NULL,
    storage_key       VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    file_size         BIGINT NOT NULL,
    note              TEXT,
    is_current        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_document_versions_document_id ON document_versions(document_id);
CREATE UNIQUE INDEX idx_document_versions_current ON document_versions(document_id) WHERE is_current = TRUE;
