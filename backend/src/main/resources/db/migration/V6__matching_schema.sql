-- MatchingService infrastructure: geo-query, availability, slot locking
-- Uses plain DOUBLE PRECISION lat/lng columns — no PostGIS extension required.
-- For production at scale, consider PostGIS with GiST index or earthdistance extension.

CREATE SCHEMA IF NOT EXISTS matching;

-- Provider service locations with plain lat/lng coordinates (WGS84 / SRID 4326)
CREATE TABLE matching.provider_locations (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id),
    address_id BIGINT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    longitude DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    service_radius_km NUMERIC(5, 2) DEFAULT 25.0,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- B-tree indexes for lat/lng range queries (replaces PostGIS GiST spatial index)
CREATE INDEX idx_provider_locations_lat_lng ON matching.provider_locations (latitude, longitude);
CREATE INDEX idx_provider_locations_provider ON matching.provider_locations (provider_id);

-- Provider unavailable dates (vacation, already booked slots, etc.)
CREATE TABLE matching.blocked_dates (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id),
    blocked_date DATE NOT NULL,
    reason VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_provider_blocked_date UNIQUE (provider_id, blocked_date)
);

CREATE INDEX idx_blocked_dates_provider
    ON matching.blocked_dates (provider_id, blocked_date);

-- DB-backed slot lock fallback: when Redis is unavailable, fall back to this table
CREATE TABLE matching.slot_locks (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    slot_date DATE NOT NULL,
    slot_time TIME NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    locked_by VARCHAR(255),  -- userId or sessionId
    locked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    released_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_slot_lock UNIQUE (provider_id, slot_date, slot_time)
);

CREATE INDEX idx_slot_locks_expires
    ON matching.slot_locks (expires_at);

CREATE INDEX idx_slot_locks_provider_slot
    ON matching.slot_locks (provider_id, slot_date, slot_time);