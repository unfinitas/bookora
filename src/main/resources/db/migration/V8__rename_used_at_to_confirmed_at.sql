-- Rename used_at column to confirmed_at in t_guest_access_token table
ALTER TABLE t_guest_access_token RENAME COLUMN used_at TO confirmed_at;

-- Rename index on confirmed_at (previously used_at)
ALTER INDEX idx_guest_access_token_used_at RENAME TO idx_guest_access_token_confirmed_at;

-- Drop old composite index that included used_at
DROP INDEX IF EXISTS idx_guest_access_token_validation;

-- Create new composite index with confirmed_at instead of used_at
CREATE INDEX idx_guest_access_token_validation ON t_guest_access_token(token, confirmed_at, expires_at);
