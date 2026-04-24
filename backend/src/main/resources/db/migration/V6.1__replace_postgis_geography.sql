-- V6.1: Replace PostGIS geography column with plain lat/lng DOUBLE PRECISION columns
-- Safe to run on fresh DB (V6 created the table but failed before populating data).
-- If location column has data, we backfill using ST_X/ST_Y on geometry cast.

-- Add new lat/lng columns
ALTER TABLE matching.provider_locations ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE matching.provider_locations ADD COLUMN longitude DOUBLE PRECISION;

-- Backfill from existing location column (if it has data)
UPDATE matching.provider_locations
SET
    latitude = ST_Y(location::geometry),
    longitude = ST_X(location::geometry)
WHERE location IS NOT NULL;

-- Make them NOT NULL (defaults to 0.0 for existing rows)
ALTER TABLE matching.provider_locations ALTER COLUMN latitude SET NOT NULL;
ALTER TABLE matching.provider_locations ALTER COLUMN latitude SET DEFAULT 0.0;
ALTER TABLE matching.provider_locations ALTER COLUMN longitude SET NOT NULL;
ALTER TABLE matching.provider_locations ALTER COLUMN longitude SET DEFAULT 0.0;

-- Drop the old PostGIS geography column and its GiST index
DROP INDEX IF EXISTS matching.idx_provider_locations_geo;
ALTER TABLE matching.provider_locations DROP COLUMN IF EXISTS location;

-- Add B-tree indexes on lat/lng for range queries (replaces GiST spatial index)
CREATE INDEX idx_provider_locations_lat_lng ON matching.provider_locations (latitude, longitude);