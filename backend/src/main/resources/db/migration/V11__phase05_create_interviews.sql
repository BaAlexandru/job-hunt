CREATE TABLE interviews (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id      UUID         NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    user_id             UUID         NOT NULL REFERENCES users(id),
    round_number        INT          NOT NULL,
    scheduled_at        TIMESTAMPTZ  NOT NULL,
    duration_minutes    INT          DEFAULT 60,
    interview_type      VARCHAR(50)  NOT NULL,
    stage               VARCHAR(50)  NOT NULL,
    stage_label         VARCHAR(255),
    outcome             VARCHAR(50)  NOT NULL DEFAULT 'SCHEDULED',
    result              VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    location            TEXT,
    interviewer_names   VARCHAR(500),
    candidate_feedback  TEXT,
    company_feedback    TEXT,
    archived            BOOLEAN      NOT NULL DEFAULT FALSE,
    archived_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_interviews_application_id ON interviews(application_id);
CREATE INDEX idx_interviews_user_id ON interviews(user_id);
CREATE INDEX idx_interviews_user_archived ON interviews(user_id, archived);
CREATE INDEX idx_interviews_scheduled_at ON interviews(application_id, scheduled_at);
CREATE INDEX idx_interviews_stage ON interviews(application_id, stage);
