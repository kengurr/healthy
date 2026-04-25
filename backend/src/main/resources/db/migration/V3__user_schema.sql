-- V3: usr schema - addresses, patients, providers, documents, provider_schedule
CREATE TABLE usr.addresses (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT,
    provider_id BIGINT,
    street VARCHAR(255) NOT NULL,
    house_number VARCHAR(255),
    apartment_number VARCHAR(255),
    city VARCHAR(255) NOT NULL,
    postal_code VARCHAR(255) NOT NULL,
    region VARCHAR(255),
    country VARCHAR(255) NOT NULL DEFAULT 'SI',
    latitude NUMERIC(38,2),
    longitude NUMERIC(38,2),
    instructions TEXT,
    is_primary BOOLEAN,
    created_at TIMESTAMP
);

CREATE TABLE usr.providers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(255),
    role VARCHAR(255) NOT NULL,
    profession VARCHAR(255) NOT NULL,
    specialty VARCHAR(255),
    rating DOUBLE PRECISION,
    reviews_count INTEGER,
    languages TEXT[],
    years_of_experience INTEGER,
    bio TEXT,
    photo_url VARCHAR(500),
    status VARCHAR(255),
    verified BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT,
    CONSTRAINT providers_role_check CHECK (role IN ('PATIENT','PROVIDER','OPERATOR','ADMIN','SUPERADMIN')),
    CONSTRAINT providers_profession_check CHECK (profession IN ('NURSE','PHYSIOTHERAPIST','DOCTOR','CAREGIVER','SOCIAL_WORKER')),
    CONSTRAINT providers_specialty_check CHECK (specialty IN ('GENERAL_CARE','WOUND_CARE','POST_SURGERY_CARE','ELDERLY_CARE','PEDIATRIC_CARE','CHRONIC_DISEASE_MANAGEMENT','REHABILITATION','PALLIATIVE_CARE','MENTAL_HEALTH')),
    CONSTRAINT providers_status_check CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','SUSPENDED','INACTIVE'))
);

CREATE TABLE usr.patients (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(255),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(255),
    insurance_provider VARCHAR(255),
    policy_number VARCHAR(255),
    group_number VARCHAR(255),
    allergies TEXT[],
    chronic_conditions TEXT[],
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(255),
    emergency_contact_relationship VARCHAR(255),
    address_street VARCHAR(255),
    address_house_number VARCHAR(255),
    address_apartment_number VARCHAR(255),
    address_city VARCHAR(255),
    address_postal_code VARCHAR(255),
    address_region VARCHAR(255),
    address_country VARCHAR(255),
    address_instructions VARCHAR(255),
    verified BOOLEAN NOT NULL DEFAULT false,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT,
    CONSTRAINT patients_gender_check CHECK (gender IN ('MALE','FEMALE','OTHER','PREFER_NOT_TO_SAY')),
    CONSTRAINT uk_patients_user_id UNIQUE (user_id)
);

CREATE TABLE usr.provider_documents (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    document_type VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(255),
    file_size BIGINT,
    uploaded_at TIMESTAMP,
    expires_at TIMESTAMP,
    verified BOOLEAN NOT NULL DEFAULT false,
    verified_by VARCHAR(255),
    verified_at TIMESTAMP,
    CONSTRAINT provider_documents_document_type_check CHECK (document_type IN ('LICENSE','CERTIFICATION','INSURANCE','ID_CARD','BACKGROUND_CHECK','OTHER'))
);

CREATE TABLE usr.patient_documents (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    document_type VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(255),
    file_size BIGINT,
    uploaded_at TIMESTAMP,
    expires_at TIMESTAMP,
    verified BOOLEAN NOT NULL DEFAULT false,
    verified_by VARCHAR(255),
    verified_at TIMESTAMP,
    CONSTRAINT patient_documents_document_type_check CHECK (document_type IN ('NATIONAL_ID','INSURANCE_CARD','REFERRAL','MEDICAL_HISTORY','CONSENT_FORM','OTHER'))
);

CREATE TABLE usr.provider_schedule (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_blocked BOOLEAN DEFAULT false,
    blocked_date DATE,
    created_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_addresses_patient_id ON usr.addresses(patient_id);
CREATE INDEX idx_addresses_provider_id ON usr.addresses(provider_id);
CREATE INDEX idx_providers_user_id ON usr.providers(user_id);
CREATE INDEX idx_providers_email ON usr.providers(email);
CREATE INDEX idx_providers_status ON usr.providers(status);
CREATE INDEX idx_providers_profession ON usr.providers(profession);
CREATE INDEX idx_providers_specialty ON usr.providers(specialty);
CREATE INDEX idx_providers_rating ON usr.providers(rating);
CREATE INDEX idx_providers_verified ON usr.providers(verified);
CREATE INDEX idx_patients_user_id ON usr.patients(user_id);
CREATE INDEX idx_patients_active ON usr.patients(active);
CREATE INDEX idx_provider_documents_provider_id ON usr.provider_documents(provider_id);
CREATE INDEX idx_patient_documents_patient_id ON usr.patient_documents(patient_id);
CREATE INDEX idx_provider_schedule_provider_id ON usr.provider_schedule(provider_id);
CREATE INDEX idx_provider_schedule_day ON usr.provider_schedule(day_of_week);
