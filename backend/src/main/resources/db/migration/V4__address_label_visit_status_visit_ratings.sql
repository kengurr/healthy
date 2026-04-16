-- V4: Address label, Visit SCHEDULED status, Visit Ratings table
-- Aligns remaining OpenAPI schema items to database
-- Created: 2026-04-16

-- =====================================================
-- ADDRESS - Add label field (OpenAPI Address.label)
-- =====================================================

ALTER TABLE "user".addresses
    ADD COLUMN IF NOT EXISTS label VARCHAR(50);

-- =====================================================
-- VISIT STATUS - Add SCHEDULED status value
-- =====================================================
-- Note: SCHEDULED is a pre-booking state before the provider starts
-- visit.visits.status column accepts any VARCHAR; no DB-level constraint.
-- The VisitStatus enum in Java entity needs to add SCHEDULED:
--   public enum VisitStatus {
--       SCHEDULED,    -- ADD THIS
--       IN_PROGRESS,
--       COMPLETED,
--       CANCELLED,
--       ESCALATED
--   }
-- Run this SQL to verify current values:
-- SELECT DISTINCT status FROM visit.visits;

-- =====================================================
-- VISIT RATINGS - Store ratings submitted by patients
-- =====================================================

CREATE TABLE IF NOT EXISTS visit.visit_ratings (
    id BIGSERIAL PRIMARY KEY,
    visit_id BIGINT NOT NULL UNIQUE REFERENCES visit.visits(id),
    patient_id BIGINT NOT NULL REFERENCES "user".patients(id),
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id),
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_visit_ratings_visit_id ON visit.visit_ratings(visit_id);
CREATE INDEX IF NOT EXISTS idx_visit_ratings_patient_id ON visit.visit_ratings(patient_id);
CREATE INDEX IF NOT EXISTS idx_visit_ratings_provider_id ON visit.visit_ratings(provider_id);

-- =====================================================
-- PROVIDER - Aggregate rating from visit_ratings
-- =====================================================
-- Note: Provider.rating and reviewsCount are currently stored columns.
-- Once visit_ratings is populated, compute aggregates:
--
-- UPDATE "user".providers p SET
--     rating = (SELECT AVG(r.rating) FROM visit.visit_ratings r WHERE r.provider_id = p.id),
--     reviews_count = (SELECT COUNT(*) FROM visit.visit_ratings r WHERE r.provider_id = p.id);
