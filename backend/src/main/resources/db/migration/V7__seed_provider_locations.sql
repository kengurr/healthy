-- Dev seed data for provider locations (Ljubljana area)
-- Run after V6__matching_schema.sql (and V6.1 which replaces geography with lat/lng columns)

-- Insert provider locations only if providers exist
INSERT INTO matching.provider_locations (provider_id, address_id, latitude, longitude, service_radius_km, is_primary)
SELECT p.id, a.id,
       a.latitude, a.longitude,
       20.0, true
FROM "user".providers p
JOIN "user".addresses a ON a.provider_id = p.id
WHERE NOT EXISTS (SELECT 1 FROM matching.provider_locations WHERE provider_id = p.id)
LIMIT 10;

-- Seed blocked dates only if providers exist
INSERT INTO matching.blocked_dates (provider_id, blocked_date, reason)
SELECT id, CURRENT_DATE + 1, 'Scheduled off'
FROM "user".providers
WHERE NOT EXISTS (
    SELECT 1 FROM matching.blocked_dates bd
    JOIN "user".providers p2 ON p2.id = bd.provider_id
    WHERE p2.id = "user".providers.id AND bd.blocked_date = CURRENT_DATE + 1
)
LIMIT 10;