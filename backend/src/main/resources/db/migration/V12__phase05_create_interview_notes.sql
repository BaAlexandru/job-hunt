CREATE TABLE interview_notes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id    UUID         NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    content         TEXT         NOT NULL,
    note_type       VARCHAR(50)  NOT NULL DEFAULT 'GENERAL',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_interview_notes_interview_id ON interview_notes(interview_id);
