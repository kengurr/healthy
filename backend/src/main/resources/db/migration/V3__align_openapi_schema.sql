-- V3: Align database schema to OpenAPI specification
-- Adds missing columns, fixes type mismatches, adds missing tables
-- Created: 2026-04-16

-- =====================================================
-- VISIT SCHEMA - Add missing clinical fields
-- =====================================================

-- Add scalar columns to visit.visits
ALTER TABLE visit.visits
    ADD COLUMN IF NOT EXISTS clinical_notes TEXT,
    ADD COLUMN IF NOT EXISTS patient_signature TEXT,
    ADD COLUMN IF NOT EXISTS report_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS gps_lat DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS gps_lng DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS notes TEXT;

-- Element collection tables for list fields (replaces TEXT[] approach)
CREATE TABLE IF NOT EXISTS visit.visit_procedures (
    visit_id BIGINT NOT NULL REFERENCES visit.visits(id) ON DELETE CASCADE,
    procedure VARCHAR(255),
    CONSTRAINT pk_visit_procedures PRIMARY KEY (visit_id, procedure)
);

CREATE TABLE IF NOT EXISTS visit.visit_photos (
    visit_id BIGINT NOT NULL REFERENCES visit.visits(id) ON DELETE CASCADE,
    photo_url VARCHAR(500),
    CONSTRAINT pk_visit_photos PRIMARY KEY (visit_id, photo_url)
);

CREATE TABLE IF NOT EXISTS visit.visit_recommendations (
    visit_id BIGINT NOT NULL REFERENCES visit.visits(id) ON DELETE CASCADE,
    recommendation VARCHAR(255),
    CONSTRAINT pk_visit_recommendations PRIMARY KEY (visit_id, recommendation)
);

-- =====================================================
-- BOOKING STATUS TIMELINE
-- =====================================================

CREATE TABLE IF NOT EXISTS booking.status_timeline (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES booking.bookings(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_status_timeline_booking_id ON booking.status_timeline(booking_id);

-- Seed initial status for existing bookings
INSERT INTO booking.status_timeline (booking_id, status, note, created_at, created_by)
SELECT b.id, b.status, 'Initial booking status', b.created_at, b.patient_id
FROM booking.bookings b
WHERE NOT EXISTS (
    SELECT 1 FROM booking.status_timeline st WHERE st.booking_id = b.id
)
ON CONFLICT DO NOTHING;

-- =====================================================
-- ESCALATION - Add GPS location and notified users
-- =====================================================

ALTER TABLE visit.escalations
    ADD COLUMN IF NOT EXISTS gps_lat DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS gps_lng DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS notified_users TEXT[];

-- =====================================================
-- CMS SCHEMA - Add missing columns
-- =====================================================

ALTER TABLE cms.service_packages
    ADD COLUMN IF NOT EXISTS service_id BIGINT,
    ADD COLUMN IF NOT EXISTS duration INTEGER,
    ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;

ALTER TABLE cms.services
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'EUR',
    ADD COLUMN IF NOT EXISTS reviews_count INTEGER DEFAULT 0;

-- Seed service_id from first element of service_ids array for existing packages
UPDATE cms.service_packages
SET service_id = service_ids[1]
WHERE service_id IS NULL AND array_length(service_ids, 1) > 0;

-- =====================================================
-- BOOKING - Add missing fields
-- =====================================================

ALTER TABLE booking.bookings
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS preferred_provider_id BIGINT REFERENCES "user".providers(id),
    ADD COLUMN IF NOT EXISTS time_slot VARCHAR(10);

-- =====================================================
-- PROVIDER AVAILABILITY SCHEDULE
-- =====================================================

CREATE TABLE IF NOT EXISTS "user".provider_availability (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id) ON DELETE CASCADE,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_provider_day UNIQUE (provider_id, day_of_week)
);

CREATE TABLE IF NOT EXISTS "user".provider_blocked_dates (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id) ON DELETE CASCADE,
    blocked_date DATE NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_provider_blocked_date UNIQUE (provider_id, blocked_date)
);

CREATE INDEX IF NOT EXISTS idx_provider_availability_provider_id ON "user".provider_availability(provider_id);
CREATE INDEX IF NOT EXISTS idx_provider_blocked_dates_provider_id ON "user".provider_blocked_dates(provider_id);

-- =====================================================
-- NOTIFICATIONS - Add created_at for OpenAPI alignment
-- =====================================================

ALTER TABLE notification.notifications
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- =====================================================
-- BILLING - Add payments table
-- =====================================================

CREATE TABLE IF NOT EXISTS billing.payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES booking.bookings(id),
    patient_id BIGINT NOT NULL REFERENCES "user".patients(id),
    provider_id BIGINT REFERENCES "user".providers(id),
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    status VARCHAR(50) DEFAULT 'PENDING',
    stripe_payment_intent_id VARCHAR(255),
    payment_method VARCHAR(50),
    refunded_amount DECIMAL(10, 2) DEFAULT 0,
    refund_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP WITH TIME ZONE,
    refunded_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_payments_booking_id ON billing.payments(booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_patient_id ON billing.payments(patient_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON billing.payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_stripe_id ON billing.payments(stripe_payment_intent_id);

ALTER TABLE billing.invoices
    ADD COLUMN IF NOT EXISTS stripe_payment_intent_id VARCHAR(255);

-- =====================================================
-- VISIT STATUS - Verify enum values match OpenAPI
-- OpenAPI: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
-- Entity also has ESCALATED (internal state)
-- =====================================================

-- Optional: ensure status enum values are consistent
-- Update any legacy values that don't match the OpenAPI set
-- This is informational - app code should handle enum conversion
