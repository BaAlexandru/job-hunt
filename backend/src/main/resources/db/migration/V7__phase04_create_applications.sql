CREATE TABLE applications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID         NOT NULL REFERENCES users(id),
    job_id              UUID         NOT NULL REFERENCES jobs(id),
    status              VARCHAR(50)  NOT NULL DEFAULT 'INTERESTED',
    quick_notes         TEXT,
    applied_date        DATE,
    last_activity_date  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    next_action_date    DATE,
    archived            BOOLEAN      NOT NULL DEFAULT FALSE,
    archived_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_applications_user_id ON applications(user_id);
CREATE INDEX idx_applications_user_archived ON applications(user_id, archived);
CREATE INDEX idx_applications_user_status ON applications(user_id, status);
CREATE INDEX idx_applications_job_id ON applications(job_id);
CREATE INDEX idx_applications_user_applied_date ON applications(user_id, applied_date);
CREATE INDEX idx_applications_user_next_action ON applications(user_id, next_action_date);
CREATE UNIQUE INDEX idx_applications_user_job ON applications(user_id, job_id) WHERE archived = false;
