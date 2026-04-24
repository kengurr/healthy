-- =====================================================
-- Zdravdom Dev Database Initialization Script
-- Run this against an EMPTY (schema-less) PostgreSQL database.
-- All schemas and tables use IF NOT EXISTS / IF NOT EXISTS for idempotency.
-- V1 base schema → V12 latest, plus seed data.
-- =====================================================

-- =====================================================
-- AUTH SCHEMA
-- =====================================================
CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE IF NOT EXISTS auth.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('PATIENT', 'PROVIDER', 'OPERATOR', 'ADMIN', 'SUPERADMIN')),
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    account_locked BOOLEAN DEFAULT FALSE,
    account_expired BOOLEAN DEFAULT FALSE,
    credentials_expired BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS auth.auth_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES auth.users(id),
    refresh_token VARCHAR(500) NOT NULL,
    device_info VARCHAR(255),
    ip_address VARCHAR(50),
    issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_auth_users_email ON auth.users(email);
CREATE INDEX IF NOT EXISTS idx_auth_users_role ON auth.users(role);
CREATE INDEX IF NOT EXISTS idx_auth_tokens_user_id ON auth.auth_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_auth_tokens_refresh_token ON auth.auth_tokens(refresh_token);

-- =====================================================
-- USER SCHEMA
-- =====================================================
CREATE SCHEMA IF NOT EXISTS "user";

CREATE TABLE IF NOT EXISTS "user".patients (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES auth.users(id),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(20),
    insurance_provider VARCHAR(100),
    policy_number VARCHAR(100),
    group_number VARCHAR(100),
    allergies TEXT[],
    chronic_conditions TEXT[],
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(50),
    emergency_contact_relationship VARCHAR(50),
    verified BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "user".addresses (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT REFERENCES "user".patients(id),
    provider_id BIGINT,
    label VARCHAR(50),
    street VARCHAR(255) NOT NULL,
    house_number VARCHAR(20),
    apartment_number VARCHAR(20),
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    region VARCHAR(100),
    country VARCHAR(2) DEFAULT 'SI',
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    instructions TEXT,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "user".providers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES auth.users(id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    role VARCHAR(50) NOT NULL,
    profession VARCHAR(50) NOT NULL,
    specialty VARCHAR(50),
    rating DECIMAL(3, 2) DEFAULT 0.0,
    reviews_count INTEGER DEFAULT 0,
    languages TEXT[] DEFAULT ARRAY['SLOVENIAN'],
    years_of_experience INTEGER,
    bio TEXT,
    photo_url VARCHAR(500),
    status VARCHAR(50) DEFAULT 'PENDING_VERIFICATION',
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "user".patient_documents (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES "user".patients(id),
    document_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    verified BOOLEAN DEFAULT FALSE,
    verified_by VARCHAR(100),
    verified_at TIMESTAMP WITH TIME ZONE
);

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

CREATE INDEX IF NOT EXISTS idx_patients_user_id ON "user".patients(user_id);
CREATE INDEX IF NOT EXISTS idx_patients_email ON "user".patients(email);
CREATE INDEX IF NOT EXISTS idx_providers_user_id ON "user".providers(user_id);
CREATE INDEX IF NOT EXISTS idx_providers_email ON "user".providers(email);
CREATE INDEX IF NOT EXISTS idx_providers_status ON "user".providers(status);
CREATE INDEX IF NOT EXISTS idx_addresses_patient_id ON "user".addresses(patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_documents_patient_id ON "user".patient_documents(patient_id);
CREATE INDEX IF NOT EXISTS idx_provider_availability_provider_id ON "user".provider_availability(provider_id);
CREATE INDEX IF NOT EXISTS idx_provider_blocked_dates_provider_id ON "user".provider_blocked_dates(provider_id);

-- =====================================================
-- BOOKING SCHEMA
-- =====================================================
CREATE SCHEMA IF NOT EXISTS booking;

CREATE TABLE IF NOT EXISTS booking.bookings (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES "user".patients(id),
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id),
    service_id BIGINT,
    package_id BIGINT,
    address_id BIGINT REFERENCES "user".addresses(id),
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(50) DEFAULT 'REQUESTED',
    payment_amount DECIMAL(10, 2),
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    cancellation_reason TEXT,
    idempotency_key VARCHAR(100) UNIQUE,
    notes TEXT,
    preferred_provider_id BIGINT REFERENCES "user".providers(id),
    time_slot VARCHAR(10),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS booking.status_timeline (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES booking.bookings(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_bookings_patient_id ON booking.bookings(patient_id);
CREATE INDEX IF NOT EXISTS idx_bookings_provider_id ON booking.bookings(provider_id);
CREATE INDEX IF NOT EXISTS idx_bookings_date ON booking.bookings(booking_date);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON booking.bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_idempotency ON booking.bookings(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_status_timeline_booking_id ON booking.status_timeline(booking_id);

-- =====================================================
-- VISIT SCHEMA
-- =====================================================
CREATE SCHEMA IF NOT EXISTS visit;

CREATE TABLE IF NOT EXISTS visit.visits (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES booking.bookings(id),
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id),
    patient_id BIGINT NOT NULL REFERENCES "user".patients(id),
    status VARCHAR(50) DEFAULT 'IN_PROGRESS',
    clinical_notes TEXT,
    patient_signature TEXT,
    report_url VARCHAR(500),
    gps_lat DOUBLE PRECISION,
    gps_lng DOUBLE PRECISION,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

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

CREATE TABLE IF NOT EXISTS visit.escalations (
    id BIGSERIAL PRIMARY KEY,
    visit_id BIGINT NOT NULL REFERENCES visit.visits(id),
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id),
    patient_id BIGINT NOT NULL REFERENCES "user".patients(id),
    urgency_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    action_taken TEXT,
    resolution TEXT,
    status VARCHAR(50) DEFAULT 'OPEN',
    gps_lat DOUBLE PRECISION,
    gps_lng DOUBLE PRECISION,
    notified_users TEXT[],
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS visit.visit_ratings (
    id BIGSERIAL PRIMARY KEY,
    visit_id BIGINT NOT NULL UNIQUE REFERENCES visit.visits(id),
    patient_id BIGINT NOT NULL REFERENCES "user".patients(id),
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id),
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Element collections
CREATE TABLE IF NOT EXISTS visit.visit_procedures (
    visit_id BIGINT NOT NULL REFERENCES visit.visits(id) ON DELETE CASCADE,
    procedure VARCHAR(255) NOT NULL,
    PRIMARY KEY (visit_id, procedure)
);

CREATE TABLE IF NOT EXISTS visit.visit_photos (
    visit_id BIGINT NOT NULL REFERENCES visit.visits(id) ON DELETE CASCADE,
    photo_url VARCHAR(500) NOT NULL,
    PRIMARY KEY (visit_id, photo_url)
);

CREATE TABLE IF NOT EXISTS visit.visit_recommendations (
    visit_id BIGINT NOT NULL REFERENCES visit.visits(id) ON DELETE CASCADE,
    recommendation VARCHAR(500) NOT NULL,
    PRIMARY KEY (visit_id, recommendation)
);

CREATE TABLE IF NOT EXISTS visit.escalation_notified_users (
    escalation_id BIGINT NOT NULL REFERENCES visit.escalations(id) ON DELETE CASCADE,
    notified_user VARCHAR(255) NOT NULL,
    PRIMARY KEY (escalation_id, notified_user)
);

CREATE INDEX IF NOT EXISTS idx_visits_booking_id ON visit.visits(booking_id);
CREATE INDEX IF NOT EXISTS idx_visits_patient_id ON visit.visits(patient_id);
CREATE INDEX IF NOT EXISTS idx_visits_provider_id ON visit.visits(provider_id);
CREATE INDEX IF NOT EXISTS idx_visits_status ON visit.visits(status);
CREATE INDEX IF NOT EXISTS idx_vitals_visit_id ON visit.vitals(visit_id);
CREATE INDEX IF NOT EXISTS idx_escalations_visit_id ON visit.escalations(visit_id);
CREATE INDEX IF NOT EXISTS idx_escalations_status ON visit.escalations(status);
CREATE INDEX IF NOT EXISTS idx_visit_ratings_visit_id ON visit.visit_ratings(visit_id);
CREATE INDEX IF NOT EXISTS idx_visit_ratings_patient_id ON visit.visit_ratings(patient_id);
CREATE INDEX IF NOT EXISTS idx_visit_ratings_provider_id ON visit.visit_ratings(provider_id);

-- =====================================================
-- CMS SCHEMA
-- =====================================================
CREATE SCHEMA IF NOT EXISTS cms;

CREATE TABLE IF NOT EXISTS cms.services (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT,
    duration_minutes INTEGER NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    rating DECIMAL(3, 2) DEFAULT 0.0,
    currency VARCHAR(3) DEFAULT 'EUR',
    image_url VARCHAR(500),
    included_items TEXT[],
    required_documents TEXT[],
    reviews_count INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cms.service_packages (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT,
    name VARCHAR(255) NOT NULL,
    size VARCHAR(10) NOT NULL,
    description TEXT,
    service_ids BIGINT[],
    price DECIMAL(10, 2) NOT NULL,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    validity_days INTEGER,
    duration INTEGER,
    benefits TEXT[],
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_services_uuid ON cms.services(uuid) WHERE uuid IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_services_category ON cms.services(category);
CREATE INDEX IF NOT EXISTS idx_services_active ON cms.services(active);
CREATE INDEX IF NOT EXISTS idx_packages_size ON cms.service_packages(size);
CREATE INDEX IF NOT EXISTS idx_packages_active ON cms.service_packages(active);

-- =====================================================
-- MATCHING SCHEMA
-- =====================================================
CREATE SCHEMA IF NOT EXISTS matching;

CREATE TABLE IF NOT EXISTS matching.provider_locations (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id),
    address_id BIGINT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    longitude DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    service_radius_km NUMERIC(5, 2) DEFAULT 25.0,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS matching.blocked_dates (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id),
    blocked_date DATE NOT NULL,
    reason VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_matching_blocked_date UNIQUE (provider_id, blocked_date)
);

CREATE TABLE IF NOT EXISTS matching.slot_locks (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    slot_date DATE NOT NULL,
    slot_time TIME NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    locked_by VARCHAR(255),
    locked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    released_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_slot_lock UNIQUE (provider_id, slot_date, slot_time)
);

CREATE INDEX IF NOT EXISTS idx_provider_locations_lat_lng ON matching.provider_locations (latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_provider_locations_provider ON matching.provider_locations (provider_id);
CREATE INDEX IF NOT EXISTS idx_blocked_dates_provider ON matching.blocked_dates (provider_id, blocked_date);
CREATE INDEX IF NOT EXISTS idx_slot_locks_expires ON matching.slot_locks (expires_at);
CREATE INDEX IF NOT EXISTS idx_slot_locks_provider_slot ON matching.slot_locks (provider_id, slot_date, slot_time);

-- =====================================================
-- BILLING SCHEMA
-- =====================================================
CREATE SCHEMA IF NOT EXISTS billing;

CREATE TABLE IF NOT EXISTS billing.invoices (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT REFERENCES booking.bookings(id),
    patient_id BIGINT NOT NULL REFERENCES "user".patients(id),
    provider_id BIGINT REFERENCES "user".providers(id),
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    tax_amount DECIMAL(10, 2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    due_date DATE,
    paid_at TIMESTAMP WITH TIME ZONE,
    stripe_payment_intent_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

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

CREATE INDEX IF NOT EXISTS idx_invoices_patient_id ON billing.invoices(patient_id);
CREATE INDEX IF NOT EXISTS idx_invoices_booking_id ON billing.invoices(booking_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON billing.invoices(status);
CREATE INDEX IF NOT EXISTS idx_payments_booking_id ON billing.payments(booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_patient_id ON billing.payments(patient_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON billing.payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_stripe_id ON billing.payments(stripe_payment_intent_id);

-- =====================================================
-- NOTIFICATION SCHEMA
-- =====================================================
CREATE SCHEMA IF NOT EXISTS notification;

CREATE TABLE IF NOT EXISTS notification.notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    data JSONB,
    read BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification.push_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    CONSTRAINT uk_push_token_token UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notification.notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notification.notifications(read);
CREATE INDEX IF NOT EXISTS idx_notifications_sent_at ON notification.notifications(sent_at);
CREATE INDEX IF NOT EXISTS idx_push_tokens_user_id ON notification.push_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_push_tokens_active ON notification.push_tokens(active);

-- =====================================================
-- AUDIT SCHEMA
-- =====================================================
CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE IF NOT EXISTS audit.health_data_access (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_role VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id BIGINT,
    patient_id BIGINT,
    ip_address VARCHAR(50),
    user_agent TEXT,
    details JSONB,
    accessed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_access_user_id ON audit.health_data_access(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_access_patient_id ON audit.health_data_access(patient_id);
CREATE INDEX IF NOT EXISTS idx_audit_access_resource ON audit.health_data_access(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_access_time ON audit.health_data_access(accessed_at);

-- =====================================================
-- SEED DATA
-- =====================================================

-- Admin user (password: Admin123!)
INSERT INTO auth.users (email, password_hash, role, enabled, account_locked, account_expired, credentials_expired, mfa_enabled)
VALUES ('admin@zdravdom.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.dnP0p4U0p3qK.VzHKe', 'SUPERADMIN', true, false, false, false, false)
ON CONFLICT (email) DO NOTHING;

-- Test provider
INSERT INTO auth.users (email, password_hash, role, enabled)
VALUES ('provider@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.dnP0p4U0p3qK.VzHKe', 'PROVIDER', true)
ON CONFLICT (email) DO NOTHING;

INSERT INTO "user".providers (user_id, first_name, last_name, email, phone, role, profession, specialty, status, verified)
VALUES (currval('auth.users_id_seq'), 'Test', 'Provider', 'provider@test.com', '+38612345678', 'PROVIDER', 'NURSE', 'GENERAL_CARE', 'ACTIVE', true)
ON CONFLICT DO NOTHING;

-- Test patient
INSERT INTO auth.users (email, password_hash, role, enabled)
VALUES ('patient@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.dnP0p4U0p3qK.VzHKe', 'PATIENT', true)
ON CONFLICT (email) DO NOTHING;

INSERT INTO "user".patients (user_id, email, first_name, last_name, phone, verified, active)
VALUES (currval('auth.users_id_seq'), 'patient@test.com', 'Test', 'Patient', '+38612345679', true, true)
ON CONFLICT DO NOTHING;

-- Sample service
INSERT INTO cms.services (name, category, description, duration_minutes, price, included_items)
VALUES ('Home Nursing Care', 'NURSING_CARE', 'Professional nursing care at home for post-operative patients and elderly.', 60, 45.00, ARRAY['Wound dressing', 'Medication administration', 'Vital signs monitoring'])
ON CONFLICT DO NOTHING;