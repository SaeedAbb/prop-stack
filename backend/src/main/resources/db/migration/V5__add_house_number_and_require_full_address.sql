ALTER TABLE address ADD COLUMN house_number VARCHAR(50);

-- Backfill pre-existing rows (seeded before house_number/state/postal_code/country were
-- mandatory) with a placeholder so the NOT NULL constraints below can be applied.
UPDATE address SET house_number = 'N/A' WHERE house_number IS NULL;
UPDATE address SET state = 'Unknown' WHERE state IS NULL;
UPDATE address SET postal_code = 'N/A' WHERE postal_code IS NULL;
UPDATE address SET country = 'Unknown' WHERE country IS NULL;

ALTER TABLE address ALTER COLUMN house_number SET NOT NULL;
ALTER TABLE address ALTER COLUMN state SET NOT NULL;
ALTER TABLE address ALTER COLUMN postal_code SET NOT NULL;
ALTER TABLE address ALTER COLUMN country SET NOT NULL;
