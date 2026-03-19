-- Baseline migration: enable extensions for UUID generation
-- gen_random_uuid() is built into PostgreSQL 13+, but pgcrypto
-- provides additional cryptographic functions for future use.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
