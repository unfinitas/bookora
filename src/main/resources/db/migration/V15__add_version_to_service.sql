-- Add version column to t_service for optimistic locking
-- This prevents lost updates when providers modify services while bookings are being created

ALTER TABLE t_service
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add comment for documentation
COMMENT ON COLUMN t_service.version IS 'Optimistic locking version field managed by JPA';
