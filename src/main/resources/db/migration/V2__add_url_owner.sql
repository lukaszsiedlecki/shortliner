-- Keycloak user ID (JWT sub) of the link's creator; NULL for anonymous and pre-existing links.
-- IF NOT EXISTS: tolerate databases where ddl-auto=update already added the column.
ALTER TABLE url_entity ADD COLUMN IF NOT EXISTS owner_id VARCHAR(36);
CREATE INDEX IF NOT EXISTS idx_url_entity_owner_id ON url_entity (owner_id);
