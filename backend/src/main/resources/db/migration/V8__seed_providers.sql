-- Add providers and addresses to zdravdom DB for matching service testing
-- Run this separately, then re-run V7 migration

INSERT INTO "user".addresses (provider_id, street, house_number, city, postal_code, region, country, latitude, longitude, is_primary)
VALUES
    (1, 'Trubarjeva', '15', 'Ljubljana', '1000', 'Ljubljana', 'SI', 46.0569, 14.5055, true),
    (2, 'Viška', '42', 'Ljubljana', '1000', 'Ljubljana', 'SI', 46.0501, 14.4815, true),
    (3, 'Šišmarska', '8', 'Ljubljana', '1000', 'Ljubljana', 'SI', 46.0706, 14.4625, true)
ON CONFLICT DO NOTHING;
