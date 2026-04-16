-- Flyway migration V1: Initial schema
-- Zdravdoma - Home Healthcare Platform
-- Created: 2026-04-15

-- =====================================================
-- AUTH SCHEMA - Authentication and authorization
-- =====================================================
CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE auth.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
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

CREATE TABLE auth.auth_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES auth.users(id),
    refresh_token VARCHAR(500) NOT NULL,
    device_info VARCHAR(255),
    ip_address VARCHAR(50),
    issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_auth_users_email ON auth.users(email);
CREATE INDEX idx_auth_users_role ON auth.users(role);
CREATE INDEX idx_auth_tokens_user_id ON auth.auth_tokens(user_id);
CREATE INDEX idx_auth_tokens_refresh_token ON auth.auth_tokens(refresh_token);

-- =====================================================
-- USER SCHEMA - Patient and Provider profiles
-- =====================================================
CREATE SCHEMA IF NOT EXISTS "user";

CREATE TABLE "user".patients (
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

CREATE TABLE "user".addresses (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT REFERENCES "user".patients(id),
    provider_id BIGINT, -- Reference to provider when needed
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

CREATE TABLE "user".providers (
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

CREATE TABLE "user".patient_documents (
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

CREATE INDEX idx_patients_user_id ON "user".patients(user_id);
CREATE INDEX idx_patients_email ON "user".patients(email);
CREATE INDEX idx_providers_user_id ON "user".providers(user_id);
CREATE INDEX idx_providers_email ON "user".providers(email);
CREATE INDEX idx_providers_status ON "user".providers(status);
CREATE INDEX idx_addresses_patient_id ON "user".addresses(patient_id);
CREATE INDEX idx_patient_documents_patient_id ON "user".patient_documents(patient_id);

-- =====================================================
-- BOOKING SCHEMA - Appointment scheduling
-- =====================================================
CREATE SCHEMA IF NOT EXISTS booking;

CREATE TABLE booking.bookings (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES "user".patients(id),
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id),
    service_id BIGINT, -- Reference to CMS service
    package_id BIGINT, -- Reference to CMS package
    address_id BIGINT REFERENCES "user".addresses(id),
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(50) DEFAULT 'REQUESTED',
    payment_amount DECIMAL(10, 2),
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    cancellation_reason TEXT,
    idempotency_key VARCHAR(100) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bookings_patient_id ON booking.bookings(patient_id);
CREATE INDEX idx_bookings_provider_id ON booking.bookings(provider_id);
CREATE INDEX idx_bookings_date ON booking.bookings(booking_date);
CREATE INDEX idx_bookings_status ON booking.bookings(status);
CREATE INDEX idx_bookings_idempotency ON booking.bookings(idempotency_key);

-- =====================================================
-- VISIT SCHEMA - Clinical documentation
-- =====================================================
CREATE SCHEMA IF NOT EXISTS visit;

CREATE TABLE visit.visits (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES booking.bookings(id),
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id),
    patient_id BIGINT NOT NULL REFERENCES "user".patients(id),
    status VARCHAR(50) DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE visit.vitals (
    id BIGSERIAL PRIMARY KEY,
    visit_id BIGINT NOT NULL REFERENCES visit.visits(id),
    blood_pressure VARCHAR(20),
    heart_rate INTEGER,
    temperature DECIMAL(4, 1),
    o2_saturation INTEGER,
    respiratory_rate INTEGER,
    blood_glucose DECIMAL(5, 1),
    weight DECIMAL(5, 2),
    notes TEXT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE visit.escalations (
    id BIGSERIAL PRIMARY KEY,
    visit_id BIGINT NOT NULL REFERENCES visit.visits(id),
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id),
    patient_id BIGINT NOT NULL REFERENCES "user".patients(id),
    urgency_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    action_taken TEXT,
    resolution TEXT,
    status VARCHAR(50) DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_visits_booking_id ON visit.visits(booking_id);
CREATE INDEX idx_visits_patient_id ON visit.visits(patient_id);
CREATE INDEX idx_visits_provider_id ON visit.visits(provider_id);
CREATE INDEX idx_visits_status ON visit.visits(status);
CREATE INDEX idx_vitals_visit_id ON visit.vitals(visit_id);
CREATE INDEX idx_escalations_visit_id ON visit.escalations(visit_id);
CREATE INDEX idx_escalations_status ON visit.escalations(status);

-- =====================================================
-- CMS SCHEMA - Content management
-- =====================================================
CREATE SCHEMA IF NOT EXISTS cms;

CREATE TABLE cms.services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT,
    duration_minutes INTEGER NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    rating DECIMAL(3, 2) DEFAULT 0.0,
    image_url VARCHAR(500),
    included_items TEXT[],
    required_documents TEXT[],
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cms.service_packages (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    size VARCHAR(10) NOT NULL,
    description TEXT,
    service_ids BIGINT[],
    price DECIMAL(10, 2) NOT NULL,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    validity_days INTEGER,
    benefits TEXT[],
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_services_category ON cms.services(category);
CREATE INDEX idx_services_active ON cms.services(active);
CREATE INDEX idx_packages_size ON cms.service_packages(size);
CREATE INDEX idx_packages_active ON cms.service_packages(active);

-- =====================================================
-- BILLING SCHEMA - Payments and invoices
-- =====================================================
CREATE SCHEMA IF NOT EXISTS billing;

CREATE TABLE billing.invoices (
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

CREATE INDEX idx_invoices_patient_id ON billing.invoices(patient_id);
CREATE INDEX idx_invoices_booking_id ON billing.invoices(booking_id);
CREATE INDEX idx_invoices_status ON billing.invoices(status);

-- =====================================================
-- NOTIFICATION SCHEMA - Alerts and messages
-- =====================================================
CREATE SCHEMA IF NOT EXISTS notification;

CREATE TABLE notification.notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_type VARCHAR(20) NOT NULL, -- 'PATIENT' or 'PROVIDER'
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    data JSONB,
    read BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notifications_user_id ON notification.notifications(user_id);
CREATE INDEX idx_notifications_read ON notification.notifications(read);
CREATE INDEX idx_notifications_sent_at ON notification.notifications(sent_at);

-- =====================================================
-- AUDIT LOG - Immutable audit trail for GDPR
-- =====================================================
CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE audit.health_data_access (
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

CREATE INDEX idx_audit_access_user_id ON audit.health_data_access(user_id);
CREATE INDEX idx_audit_access_patient_id ON audit.health_data_access(patient_id);
CREATE INDEX idx_audit_access_resource ON audit.health_data_access(resource_type, resource_id);
CREATE INDEX idx_audit_access_time ON audit.health_data_access(accessed_at);

-- =====================================================
-- SOFT DELETE HELPERS (for 10-year retention policy)
-- =====================================================
-- All tables should use updated_at for soft delete markers
-- For sensitive health data, use anonymization before hard delete

COMMENT ON SCHEMA auth IS 'Authentication and authorization - users, roles, JWT tokens';
COMMENT ON SCHEMA "user" IS 'Patient and Provider profiles, addresses, documents';
COMMENT ON SCHEMA booking IS 'Appointment scheduling and time slots';
COMMENT ON SCHEMA visit IS 'Clinical visits, vitals, escalations';
COMMENT ON SCHEMA cms IS 'Services, packages, pricing, content management';
COMMENT ON SCHEMA billing IS 'Invoices, payments, Stripe integration';
COMMENT ON SCHEMA notification IS 'Push notifications, SMS, email';
COMMENT ON SCHEMA audit IS 'Immutable audit logs for GDPR compliance';
