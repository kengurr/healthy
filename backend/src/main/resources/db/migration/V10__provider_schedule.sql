-- Provider weekly schedule and blocked dates for availability management.
-- Used by ProviderService to store per-provider weekly time slots and exceptions.

CREATE TABLE provider_schedule (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL REFERENCES "user".providers(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,          -- MONDAY, TUESDAY, ...
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_blocked BOOLEAN DEFAULT FALSE,           -- TRUE = blocked date, FALSE = available slot
    blocked_date DATE,                         -- set when is_blocked = TRUE
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_provider_day_slot UNIQUE (provider_id, day_of_week, start_time, end_time)
);

CREATE INDEX idx_provider_schedule_provider ON provider_schedule(provider_id);
CREATE INDEX idx_provider_schedule_blocked ON provider_schedule(provider_id, is_blocked, blocked_date) WHERE is_blocked = TRUE;
