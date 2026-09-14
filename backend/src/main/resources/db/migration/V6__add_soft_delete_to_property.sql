-- Soft-delete support for Property: a non-null deleted_at marks a property as deleted without
-- removing its row (or its owned Address row, via the existing cascade), so it can be restored.
-- There is no hard-delete/purge feature - deleted rows are kept indefinitely.
ALTER TABLE property ADD COLUMN deleted_at TIMESTAMP;

-- Speeds up both the "Owned" (deleted_at IS NULL) and "Deleted" (deleted_at IS NOT NULL) list
-- queries, which are always additionally scoped by organization_id.
CREATE INDEX idx_property_organization_id_deleted_at ON property (organization_id, deleted_at);
