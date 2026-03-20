CREATE TABLE token_blocklist (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_id   VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_token_blocklist_token_id ON token_blocklist(token_id);
CREATE INDEX idx_token_blocklist_expires_at ON token_blocklist(expires_at);
