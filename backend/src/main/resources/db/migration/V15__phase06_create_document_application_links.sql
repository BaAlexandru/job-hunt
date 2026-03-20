CREATE TABLE document_application_links (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_version_id   UUID NOT NULL REFERENCES document_versions(id),
    application_id        UUID NOT NULL REFERENCES applications(id),
    linked_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_doc_app_links_version ON document_application_links(document_version_id);
CREATE INDEX idx_doc_app_links_application ON document_application_links(application_id);
CREATE UNIQUE INDEX idx_doc_app_links_unique ON document_application_links(document_version_id, application_id);
