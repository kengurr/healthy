-- Add UUID column to cms.services for API-level UUID identification.
-- This allows matching endpoints to use service UUID instead of internal Long ID.

ALTER TABLE cms.services ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();

-- Make uuid unique and non-null for existing rows
UPDATE cms.services SET uuid = gen_random_uuid() WHERE uuid IS NULL;
ALTER TABLE cms.services ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE cms.services ALTER COLUMN uuid SET DEFAULT gen_random_uuid();

CREATE UNIQUE INDEX IF NOT EXISTS idx_services_uuid ON cms.services(uuid);
