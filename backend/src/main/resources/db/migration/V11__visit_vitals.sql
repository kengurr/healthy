-- Vitals table for clinical vital signs recorded during visits.
-- visit.vitals already exists as a Hibernate auto-created table since Vitals entity
-- was created before V9. This migration is a no-op for existing installs.

-- The visit.vitals table should already exist from Hibernate auto-creation.
-- If it doesn't exist (e.g., fresh DB), create it here:
CREATE TABLE IF NOT EXISTS visit.vitals (
    id BIGSERIAL PRIMARY KEY,
    visit_id BIGINT NOT NULL REFERENCES visit.visits(id),
    blood_pressure VARCHAR(20),
    heart_rate INTEGER,
    temperature NUMERIC(4, 1),
    o2_saturation INTEGER,
    respiratory_rate INTEGER,
    blood_glucose NUMERIC(5, 1),
    weight NUMERIC(5, 2),
    notes TEXT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vitals_visit ON visit.vitals(visit_id);
