ALTER TABLE property ALTER COLUMN address_id SET NOT NULL;
ALTER TABLE property ADD CONSTRAINT uq_property_address_id UNIQUE (address_id);
ALTER TABLE property ADD CONSTRAINT fk_property_address FOREIGN KEY (address_id) REFERENCES address (id);
ALTER TABLE property DROP COLUMN address;
