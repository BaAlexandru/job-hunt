CREATE TABLE application_notes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID         NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    content         TEXT         NOT NULL,
    note_type       VARCHAR(50)  NOT NULL DEFAULT 'GENERAL',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_app_notes_application_id ON application_notes(application_id);
CREATE INDEX idx_app_notes_note_type ON application_notes(application_id, note_type);
