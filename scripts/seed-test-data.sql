-- Seed test data for test01@test.conm
-- User ID: 69a36caa-1435-4a0e-91ec-61b733c0cc7b

DO $$
DECLARE
    uid UUID := '69a36caa-1435-4a0e-91ec-61b733c0cc7b';
    company_ids UUID[];
    job_ids UUID[];
    cid UUID;
    jid UUID;
    i INT;
BEGIN

-- ============================================================================
-- 50 COMPANIES
-- ============================================================================

-- Tech Giants
INSERT INTO companies (id, user_id, name, website, location, notes, created_at, updated_at) VALUES
(gen_random_uuid(), uid, 'Google', 'https://careers.google.com', 'Mountain View, CA', 'Alphabet subsidiary. Applied to Cloud division.', now() - interval '45 days', now() - interval '2 days'),
(gen_random_uuid(), uid, 'Microsoft', 'https://careers.microsoft.com', 'Redmond, WA', 'Azure team hiring aggressively.', now() - interval '44 days', now() - interval '3 days'),
(gen_random_uuid(), uid, 'Amazon', 'https://amazon.jobs', 'Seattle, WA', 'AWS division. Bar raiser interview process.', now() - interval '43 days', now() - interval '5 days'),
(gen_random_uuid(), uid, 'Apple', 'https://jobs.apple.com', 'Cupertino, CA', 'Services team. Very secretive process.', now() - interval '42 days', now() - interval '10 days'),
(gen_random_uuid(), uid, 'Meta', 'https://metacareers.com', 'Menlo Park, CA', 'Reality Labs division.', now() - interval '41 days', now() - interval '1 day'),
(gen_random_uuid(), uid, 'Netflix', 'https://jobs.netflix.com', 'Los Gatos, CA', 'Streaming infrastructure team.', now() - interval '40 days', now() - interval '4 days'),
(gen_random_uuid(), uid, 'Salesforce', 'https://salesforce.com/careers', 'San Francisco, CA', 'Platform engineering roles.', now() - interval '39 days', now() - interval '6 days'),
(gen_random_uuid(), uid, 'Oracle', 'https://oracle.com/careers', 'Austin, TX', 'OCI cloud team.', now() - interval '38 days', now() - interval '7 days'),
(gen_random_uuid(), uid, 'IBM', 'https://ibm.com/careers', 'Armonk, NY', 'Watson AI division.', now() - interval '37 days', now() - interval '8 days'),
(gen_random_uuid(), uid, 'Adobe', 'https://adobe.com/careers', 'San Jose, CA', 'Creative Cloud backend.', now() - interval '36 days', now() - interval '9 days');

-- Mid-size Tech
INSERT INTO companies (id, user_id, name, website, location, notes, created_at, updated_at) VALUES
(gen_random_uuid(), uid, 'Stripe', 'https://stripe.com/jobs', 'San Francisco, CA', 'Payments infrastructure. Great eng culture.', now() - interval '35 days', now() - interval '2 days'),
(gen_random_uuid(), uid, 'Datadog', 'https://datadoghq.com/careers', 'New York, NY', 'Observability platform.', now() - interval '34 days', now() - interval '3 days'),
(gen_random_uuid(), uid, 'Snowflake', 'https://snowflake.com/careers', 'Bozeman, MT', 'Data warehouse. Remote-friendly.', now() - interval '33 days', now() - interval '4 days'),
(gen_random_uuid(), uid, 'Cloudflare', 'https://cloudflare.com/careers', 'San Francisco, CA', 'Edge computing and CDN.', now() - interval '32 days', now() - interval '5 days'),
(gen_random_uuid(), uid, 'Twilio', 'https://twilio.com/company/jobs', 'San Francisco, CA', 'Communications APIs.', now() - interval '31 days', now() - interval '6 days'),
(gen_random_uuid(), uid, 'Square (Block)', 'https://block.xyz/careers', 'San Francisco, CA', 'Fintech. Cash App team.', now() - interval '30 days', now() - interval '7 days'),
(gen_random_uuid(), uid, 'Shopify', 'https://shopify.com/careers', 'Ottawa, Canada', 'E-commerce platform. Fully remote.', now() - interval '29 days', now() - interval '8 days'),
(gen_random_uuid(), uid, 'Atlassian', 'https://atlassian.com/company/careers', 'Sydney, Australia', 'Dev tools. TEAM Anywhere policy.', now() - interval '28 days', now() - interval '9 days'),
(gen_random_uuid(), uid, 'GitLab', 'https://gitlab.com/jobs', 'Remote', 'All-remote company. DevOps platform.', now() - interval '27 days', now() - interval '10 days'),
(gen_random_uuid(), uid, 'HashiCorp', 'https://hashicorp.com/careers', 'San Francisco, CA', 'Infrastructure automation. Terraform team.', now() - interval '26 days', now() - interval '11 days');

-- Startups & Scale-ups
INSERT INTO companies (id, user_id, name, website, location, notes, created_at, updated_at) VALUES
(gen_random_uuid(), uid, 'Vercel', 'https://vercel.com/careers', 'San Francisco, CA', 'Next.js creators. Frontend cloud.', now() - interval '25 days', now() - interval '1 day'),
(gen_random_uuid(), uid, 'Supabase', 'https://supabase.com/careers', 'Remote', 'Open source Firebase alternative.', now() - interval '24 days', now() - interval '2 days'),
(gen_random_uuid(), uid, 'PlanetScale', 'https://planetscale.com/careers', 'Remote', 'Serverless MySQL. Vitess-based.', now() - interval '23 days', now() - interval '3 days'),
(gen_random_uuid(), uid, 'Railway', 'https://railway.app/careers', 'San Francisco, CA', 'Developer platform. Small team.', now() - interval '22 days', now() - interval '4 days'),
(gen_random_uuid(), uid, 'Fly.io', 'https://fly.io/jobs', 'Remote', 'Edge app platform. Rust-heavy.', now() - interval '21 days', now() - interval '5 days'),
(gen_random_uuid(), uid, 'Temporal', 'https://temporal.io/careers', 'Remote', 'Workflow orchestration. Great OSS community.', now() - interval '20 days', now() - interval '6 days'),
(gen_random_uuid(), uid, 'Grafana Labs', 'https://grafana.com/careers', 'Remote', 'Observability stack. LGTM.', now() - interval '19 days', now() - interval '7 days'),
(gen_random_uuid(), uid, 'Cockroach Labs', 'https://cockroachlabs.com/careers', 'New York, NY', 'Distributed SQL. CockroachDB.', now() - interval '18 days', now() - interval '8 days'),
(gen_random_uuid(), uid, 'Airbyte', 'https://airbyte.com/careers', 'San Francisco, CA', 'Data integration platform. OSS.', now() - interval '17 days', now() - interval '9 days'),
(gen_random_uuid(), uid, 'Retool', 'https://retool.com/careers', 'San Francisco, CA', 'Internal tool builder. Series C.', now() - interval '16 days', now() - interval '10 days');

-- Enterprise & Consulting
INSERT INTO companies (id, user_id, name, website, location, notes, created_at, updated_at) VALUES
(gen_random_uuid(), uid, 'Palantir', 'https://palantir.com/careers', 'Denver, CO', 'Data analytics. Government + commercial.', now() - interval '15 days', now() - interval '1 day'),
(gen_random_uuid(), uid, 'Databricks', 'https://databricks.com/careers', 'San Francisco, CA', 'Lakehouse platform. Apache Spark.', now() - interval '14 days', now() - interval '2 days'),
(gen_random_uuid(), uid, 'Confluent', 'https://confluent.io/careers', 'Mountain View, CA', 'Kafka creators. Event streaming.', now() - interval '13 days', now() - interval '3 days'),
(gen_random_uuid(), uid, 'Elastic', 'https://elastic.co/careers', 'Remote', 'Elasticsearch. Observability + security.', now() - interval '12 days', now() - interval '4 days'),
(gen_random_uuid(), uid, 'MongoDB', 'https://mongodb.com/careers', 'New York, NY', 'Document database. Atlas cloud.', now() - interval '11 days', now() - interval '5 days'),
(gen_random_uuid(), uid, 'Redis Inc', 'https://redis.io/careers', 'Mountain View, CA', 'In-memory data store.', now() - interval '10 days', now() - interval '6 days'),
(gen_random_uuid(), uid, 'Akamai', 'https://akamai.com/careers', 'Cambridge, MA', 'CDN and security. Large scale.', now() - interval '9 days', now() - interval '7 days'),
(gen_random_uuid(), uid, 'Palo Alto Networks', 'https://paloaltonetworks.com/careers', 'Santa Clara, CA', 'Cybersecurity. Prisma Cloud team.', now() - interval '8 days', now() - interval '8 days'),
(gen_random_uuid(), uid, 'CrowdStrike', 'https://crowdstrike.com/careers', 'Austin, TX', 'Endpoint security. Falcon platform.', now() - interval '7 days', now() - interval '7 days'),
(gen_random_uuid(), uid, 'Zscaler', 'https://zscaler.com/careers', 'San Jose, CA', 'Zero trust security.', now() - interval '6 days', now() - interval '6 days');

-- Fintech & Other
INSERT INTO companies (id, user_id, name, website, location, notes, created_at, updated_at) VALUES
(gen_random_uuid(), uid, 'Plaid', 'https://plaid.com/careers', 'San Francisco, CA', 'Financial data APIs.', now() - interval '5 days', now() - interval '1 day'),
(gen_random_uuid(), uid, 'Brex', 'https://brex.com/careers', 'San Francisco, CA', 'Corporate cards for startups.', now() - interval '5 days', now() - interval '2 days'),
(gen_random_uuid(), uid, 'Rippling', 'https://rippling.com/careers', 'San Francisco, CA', 'HR + IT platform. Fast growing.', now() - interval '4 days', now() - interval '1 day'),
(gen_random_uuid(), uid, 'Figma', 'https://figma.com/careers', 'San Francisco, CA', 'Design tool. Web-based.', now() - interval '4 days', now() - interval '2 days'),
(gen_random_uuid(), uid, 'Notion', 'https://notion.so/careers', 'San Francisco, CA', 'Productivity tool. API team.', now() - interval '3 days', now() - interval '1 day'),
(gen_random_uuid(), uid, 'Linear', 'https://linear.app/careers', 'Remote', 'Project management. Superb UX.', now() - interval '3 days', now() - interval '2 days'),
(gen_random_uuid(), uid, 'Postman', 'https://postman.com/careers', 'San Francisco, CA', 'API platform.', now() - interval '2 days', now() - interval '1 day'),
(gen_random_uuid(), uid, 'Miro', 'https://miro.com/careers', 'San Francisco, CA', 'Visual collaboration platform.', now() - interval '2 days', now() - interval '1 day'),
(gen_random_uuid(), uid, 'Canva', 'https://canva.com/careers', 'Sydney, Australia', 'Design platform. Massive scale.', now() - interval '1 day', now() - interval '1 day'),
(gen_random_uuid(), uid, 'Discord', 'https://discord.com/careers', 'San Francisco, CA', 'Chat platform. Elixir/Rust stack.', now() - interval '1 day', now() - interval '1 day');

-- Collect all company IDs for this user
SELECT array_agg(id ORDER BY created_at) INTO company_ids FROM companies WHERE user_id = uid;

-- ============================================================================
-- 100 JOBS (2 per company)
-- ============================================================================

-- We'll create 2 jobs per company with varied attributes
FOR i IN 1..50 LOOP
    cid := company_ids[i];

    -- Job 1: Backend/Platform role
    INSERT INTO jobs (id, user_id, company_id, title, description, url, notes, location, work_mode, job_type, salary_type, salary_min, salary_max, currency, salary_period, closing_date, created_at, updated_at) VALUES
    (gen_random_uuid(), uid, cid,
     CASE (i % 10)
       WHEN 0 THEN 'Senior Backend Engineer'
       WHEN 1 THEN 'Staff Software Engineer'
       WHEN 2 THEN 'Platform Engineer'
       WHEN 3 THEN 'Senior Software Engineer'
       WHEN 4 THEN 'Backend Engineer II'
       WHEN 5 THEN 'Principal Engineer'
       WHEN 6 THEN 'Software Engineer - Infrastructure'
       WHEN 7 THEN 'Senior Platform Engineer'
       WHEN 8 THEN 'Lead Backend Developer'
       ELSE 'Software Engineer III'
     END,
     'We are looking for an experienced engineer to join our team and help build scalable distributed systems.',
     'https://example.com/jobs/' || i,
     CASE WHEN i % 3 = 0 THEN 'Referred by a friend who works there.' WHEN i % 5 = 0 THEN 'Interesting tech stack.' ELSE NULL END,
     CASE (i % 5)
       WHEN 0 THEN 'San Francisco, CA'
       WHEN 1 THEN 'New York, NY'
       WHEN 2 THEN 'Remote'
       WHEN 3 THEN 'Seattle, WA'
       ELSE 'Austin, TX'
     END,
     CASE (i % 4) WHEN 0 THEN 'REMOTE' WHEN 1 THEN 'HYBRID' WHEN 2 THEN 'ONSITE' ELSE 'REMOTE' END,
     'FULL_TIME',
     'RANGE',
     (120000 + (i * 2000))::numeric,
     (180000 + (i * 3000))::numeric,
     'USD',
     'ANNUAL',
     CASE WHEN i % 4 = 0 THEN (current_date + (i || ' days')::interval)::date ELSE NULL END,
     now() - ((50 - i) || ' days')::interval,
     now() - ((25 - i/2) || ' days')::interval
    );

    -- Job 2: Frontend/Full-stack role
    INSERT INTO jobs (id, user_id, company_id, title, description, url, notes, location, work_mode, job_type, salary_type, salary_min, salary_max, currency, salary_period, closing_date, created_at, updated_at) VALUES
    (gen_random_uuid(), uid, cid,
     CASE (i % 10)
       WHEN 0 THEN 'Senior Frontend Engineer'
       WHEN 1 THEN 'Full Stack Developer'
       WHEN 2 THEN 'React Engineer'
       WHEN 3 THEN 'Senior Full Stack Engineer'
       WHEN 4 THEN 'Frontend Engineer II'
       WHEN 5 THEN 'UI Engineer'
       WHEN 6 THEN 'TypeScript Developer'
       WHEN 7 THEN 'Senior React Developer'
       WHEN 8 THEN 'Frontend Architect'
       ELSE 'Full Stack Engineer III'
     END,
     'Join our frontend team to build delightful user experiences with modern web technologies.',
     'https://example.com/jobs/' || (100 + i),
     CASE WHEN i % 4 = 0 THEN 'Good Glassdoor reviews.' WHEN i % 7 = 0 THEN 'Competitive benefits package.' ELSE NULL END,
     CASE (i % 5)
       WHEN 0 THEN 'Remote'
       WHEN 1 THEN 'San Francisco, CA'
       WHEN 2 THEN 'London, UK'
       WHEN 3 THEN 'Remote'
       ELSE 'Berlin, Germany'
     END,
     CASE (i % 3) WHEN 0 THEN 'REMOTE' WHEN 1 THEN 'HYBRID' ELSE 'ONSITE' END,
     CASE WHEN i % 8 = 0 THEN 'CONTRACT' ELSE 'FULL_TIME' END,
     'RANGE',
     (110000 + (i * 1500))::numeric,
     (170000 + (i * 2500))::numeric,
     CASE WHEN i % 5 = 2 THEN 'GBP' WHEN i % 5 = 4 THEN 'EUR' ELSE 'USD' END,
     'ANNUAL',
     CASE WHEN i % 5 = 0 THEN (current_date + (i * 2 || ' days')::interval)::date ELSE NULL END,
     now() - ((48 - i) || ' days')::interval,
     now() - ((24 - i/2) || ' days')::interval
    );
END LOOP;

-- Collect all job IDs for this user
SELECT array_agg(id ORDER BY created_at) INTO job_ids FROM jobs WHERE user_id = uid;

-- ============================================================================
-- 50 APPLICATIONS (spread across statuses)
-- ============================================================================

-- Use the first 50 jobs for applications with a realistic status distribution:
-- INTERESTED: 8, APPLIED: 12, PHONE_SCREEN: 6, INTERVIEW: 8, OFFER: 3, REJECTED: 7, ACCEPTED: 2, WITHDRAWN: 4

FOR i IN 1..50 LOOP
    jid := job_ids[i];

    INSERT INTO applications (id, user_id, job_id, status, quick_notes, applied_date, last_activity_date, next_action_date, created_at, updated_at) VALUES
    (gen_random_uuid(), uid, jid,
     -- Status distribution
     CASE
       WHEN i <= 8 THEN 'INTERESTED'
       WHEN i <= 20 THEN 'APPLIED'
       WHEN i <= 26 THEN 'PHONE_SCREEN'
       WHEN i <= 34 THEN 'INTERVIEW'
       WHEN i <= 37 THEN 'OFFER'
       WHEN i <= 44 THEN 'REJECTED'
       WHEN i <= 46 THEN 'ACCEPTED'
       ELSE 'WITHDRAWN'
     END,
     -- Quick notes
     CASE
       WHEN i % 5 = 0 THEN 'Follow up next week.'
       WHEN i % 7 = 0 THEN 'Need to prepare system design.'
       WHEN i % 3 = 0 THEN 'Recruiter was responsive.'
       WHEN i % 11 = 0 THEN 'Interesting team and project.'
       ELSE NULL
     END,
     -- Applied date (NULL for INTERESTED)
     CASE
       WHEN i <= 8 THEN NULL
       ELSE (current_date - ((50 - i) || ' days')::interval)::date
     END,
     -- Last activity date
     now() - ((50 - i) || ' days')::interval + (i || ' hours')::interval,
     -- Next action date (for active statuses)
     CASE
       WHEN i <= 8 THEN (current_date + ((i * 2) || ' days')::interval)::date
       WHEN i <= 34 THEN (current_date + ((i) || ' days')::interval)::date
       WHEN i <= 37 THEN (current_date + '3 days'::interval)::date
       ELSE NULL
     END,
     now() - ((50 - i) || ' days')::interval,
     now() - ((25 - i/2) || ' days')::interval
    );
END LOOP;

RAISE NOTICE 'Seeded: % companies, % jobs, 50 applications', array_length(company_ids, 1), array_length(job_ids, 1);

END $$;
