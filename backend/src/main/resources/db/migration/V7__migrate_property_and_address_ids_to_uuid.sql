-- Switch Property/Address primary keys from sequential BIGINT IDENTITY to UUID, so opaque ids
-- are exposed to the frontend instead of leaking sequential row counts. Preserves the existing
-- property<->address FK relationship and all pre-existing rows (dev seed data included) rather
-- than dropping/recreating the tables.

ALTER TABLE address ADD COLUMN uuid_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE property ADD COLUMN uuid_id UUID NOT NULL DEFAULT gen_random_uuid();

-- Backfill the new FK column via the old bigint relationship before it's dropped.
ALTER TABLE property ADD COLUMN address_uuid_id UUID;
UPDATE property p SET address_uuid_id = a.uuid_id FROM address a WHERE p.address_id = a.id;
ALTER TABLE property ALTER COLUMN address_uuid_id SET NOT NULL;

ALTER TABLE property DROP CONSTRAINT fk_property_address;
ALTER TABLE property DROP CONSTRAINT uq_property_address_id;
ALTER TABLE property DROP CONSTRAINT property_pkey;
ALTER TABLE address DROP CONSTRAINT address_pkey;

ALTER TABLE property DROP COLUMN address_id;
ALTER TABLE property DROP COLUMN id;
ALTER TABLE address DROP COLUMN id;

ALTER TABLE property RENAME COLUMN uuid_id TO id;
ALTER TABLE property RENAME COLUMN address_uuid_id TO address_id;
ALTER TABLE address RENAME COLUMN uuid_id TO id;

ALTER TABLE property ADD PRIMARY KEY (id);
ALTER TABLE address ADD PRIMARY KEY (id);
ALTER TABLE property ADD CONSTRAINT uq_property_address_id UNIQUE (address_id);
ALTER TABLE property ADD CONSTRAINT fk_property_address FOREIGN KEY (address_id) REFERENCES address (id);
