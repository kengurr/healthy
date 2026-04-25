-- V5: matching schema - slot_locks, blocked_dates, provider_locations
CREATE TABLE matching.slot_locks (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    slot_date DATE NOT NULL,
    slot_time TIME NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    locked_by VARCHAR(255),
    locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    released_at TIMESTAMP
);

CREATE TABLE matching.blocked_dates (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    blocked_date DATE NOT NULL,
    reason VARCHAR(100),
    created_at TIMESTAMP
);

CREATE TABLE matching.provider_locations (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    address_id BIGINT,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    service_radius_km NUMERIC(5,2) DEFAULT 10.0,
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_slot_locks_provider_slot ON matching.slot_locks(provider_id, slot_date, slot_time);
CREATE INDEX idx_slot_locks_expires_at ON matching.slot_locks(expires_at);
CREATE INDEX idx_slot_locks_idempotency ON matching.slot_locks(idempotency_key);
CREATE INDEX idx_blocked_dates_provider_id ON matching.blocked_dates(provider_id);
CREATE INDEX idx_blocked_dates_date ON matching.blocked_dates(blocked_date);
CREATE INDEX idx_provider_locations_provider_id ON matching.provider_locations(provider_id);
CREATE INDEX idx_provider_locations_coords ON matching.provider_locations(latitude, longitude);
