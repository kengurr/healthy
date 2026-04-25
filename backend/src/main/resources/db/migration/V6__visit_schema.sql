-- V6: visit schema - visits, vitals, visit_ratings, escalations, element collections
CREATE TABLE visit.visits (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT,
    provider_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    status VARCHAR(50) DEFAULT 'SCHEDULED' NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    scheduled_at TIMESTAMP,
    clinical_notes TEXT,
    patient_signature TEXT,
    report_url VARCHAR(500),
    gps_latitude DOUBLE PRECISION,
    gps_longitude DOUBLE PRECISION,
    notes TEXT,
    visit_procedures TEXT[],
    visit_photos TEXT[],
    visit_recommendations TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0,
    gps_lat DOUBLE PRECISION,
    gps_lng DOUBLE PRECISION
);

CREATE TABLE visit.vitals (
    id BIGSERIAL PRIMARY KEY,
    visit_id BIGINT NOT NULL,
    blood_pressure VARCHAR(20),
    heart_rate INTEGER,
    temperature NUMERIC(4,1),
    o2_saturation INTEGER,
    respiratory_rate INTEGER,
    blood_glucose NUMERIC(5,1),
    weight NUMERIC(5,2),
    notes TEXT,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE visit.visit_ratings (
    id BIGSERIAL PRIMARY KEY,
    visit_id BIGINT NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE visit.escalations (
    id BIGSERIAL PRIMARY KEY,
    visit_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    urgency_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    action_taken TEXT,
    resolution TEXT,
    status VARCHAR(50) DEFAULT 'OPEN',
    gps_latitude DOUBLE PRECISION,
    gps_longitude DOUBLE PRECISION,
    notified_users TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gps_lat DOUBLE PRECISION,
    gps_lng DOUBLE PRECISION,
    resolved_at TIMESTAMP
);

-- Element collection tables
CREATE TABLE visit.visit_visit_procedures (
    visit_id BIGINT NOT NULL,
    procedure VARCHAR(255),
    CONSTRAINT fk_visit_procedures_visit FOREIGN KEY (visit_id) REFERENCES visit.visits(id)
);

CREATE TABLE visit.visit_visit_photos (
    visit_id BIGINT NOT NULL,
    photo_url VARCHAR(255),
    CONSTRAINT fk_visit_photos_visit FOREIGN KEY (visit_id) REFERENCES visit.visits(id)
);

CREATE TABLE visit.visit_visit_recommendations (
    visit_id BIGINT NOT NULL,
    recommendation VARCHAR(255),
    CONSTRAINT fk_visit_recommendations_visit FOREIGN KEY (visit_id) REFERENCES visit.visits(id)
);

CREATE TABLE visit.visit_escalation_notified_users (
    escalation_id BIGINT NOT NULL,
    notified_user VARCHAR(255),
    CONSTRAINT fk_escalation_notified_users FOREIGN KEY (escalation_id) REFERENCES visit.escalations(id)
);

-- Indexes
CREATE INDEX idx_visits_booking_id ON visit.visits(booking_id);
CREATE INDEX idx_visits_provider_id ON visit.visits(provider_id);
CREATE INDEX idx_visits_patient_id ON visit.visits(patient_id);
CREATE INDEX idx_visits_status ON visit.visits(status);
CREATE INDEX idx_visits_scheduled_at ON visit.visits(scheduled_at);
CREATE INDEX idx_visits_created_at ON visit.visits(created_at);
CREATE INDEX idx_visits_completed_at ON visit.visits(completed_at);
CREATE INDEX idx_vitals_visit_id ON visit.vitals(visit_id);
CREATE INDEX idx_vitals_recorded_at ON visit.vitals(recorded_at);
CREATE INDEX idx_visit_ratings_visit_id ON visit.visit_ratings(visit_id);
CREATE INDEX idx_visit_ratings_patient_id ON visit.visit_ratings(patient_id);
CREATE INDEX idx_visit_ratings_provider_id ON visit.visit_ratings(provider_id);
CREATE INDEX idx_escalations_visit_id ON visit.escalations(visit_id);
CREATE INDEX idx_escalations_provider_id ON visit.escalations(provider_id);
CREATE INDEX idx_escalations_urgency ON visit.escalations(urgency_type);
CREATE INDEX idx_escalations_status ON visit.escalations(status);
