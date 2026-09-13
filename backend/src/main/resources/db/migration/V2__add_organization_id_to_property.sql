ALTER TABLE property ADD COLUMN organization_id VARCHAR(255) NOT NULL;

CREATE INDEX idx_property_organization_id ON property (organization_id);
