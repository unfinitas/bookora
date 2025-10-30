-- Add version column to t_guest_access_token for optimistic locking
-- This prevents lost updates when tokens are confirmed while being expired

ALTER TABLE t_guest_access_token
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add comment for documentation
COMMENT ON COLUMN t_guest_access_token.version IS 'Optimistic locking version field managed by JPA';
