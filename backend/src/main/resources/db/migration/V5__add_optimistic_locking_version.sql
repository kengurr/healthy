-- V5: Add optimistic locking version column to patients and providers
-- Enables @Version-based concurrency control and @DynamicUpdate performance
-- Non-breaking: default 0 allows existing rows to work, new rows auto-increment

ALTER TABLE "user".patients
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

ALTER TABLE "user".providers
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
