-- Add visibility column to companies (all existing rows default to PRIVATE)
ALTER TABLE companies ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';

-- Add visibility column to jobs (all existing rows default to PRIVATE)
ALTER TABLE jobs ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';

-- Shares join table (polymorphic: resource_type + resource_id)
CREATE TABLE resource_shares (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_type   VARCHAR(20) NOT NULL,
    resource_id     UUID NOT NULL,
    owner_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shared_with_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_resource_share UNIQUE (resource_type, resource_id, shared_with_id)
);

CREATE INDEX idx_shares_resource ON resource_shares(resource_type, resource_id);
CREATE INDEX idx_shares_shared_with ON resource_shares(shared_with_id, resource_type);
