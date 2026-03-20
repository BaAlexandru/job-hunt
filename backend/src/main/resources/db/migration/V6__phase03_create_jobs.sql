CREATE TABLE jobs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id),
    company_id    UUID         REFERENCES companies(id),
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    url           VARCHAR(2000),
    notes         TEXT,
    location      VARCHAR(255),
    work_mode     VARCHAR(50),
    job_type      VARCHAR(50),
    salary_type   VARCHAR(50),
    salary_min    NUMERIC(15, 2),
    salary_max    NUMERIC(15, 2),
    salary_text   VARCHAR(255),
    currency      VARCHAR(10),
    salary_period VARCHAR(50),
    closing_date  DATE,
    archived      BOOLEAN      NOT NULL DEFAULT FALSE,
    archived_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_jobs_user_id ON jobs(user_id);
CREATE INDEX idx_jobs_user_archived ON jobs(user_id, archived);
CREATE INDEX idx_jobs_company_id ON jobs(company_id);
CREATE INDEX idx_jobs_user_job_type ON jobs(user_id, job_type);
CREATE INDEX idx_jobs_user_work_mode ON jobs(user_id, work_mode);
