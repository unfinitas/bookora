-- Add version column to t_booking for optimistic locking
-- This prevents lost updates when multiple users/threads attempt to modify the same booking
-- JPA will automatically check and increment this version on each update

ALTER TABLE t_booking
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add comment for documentation
COMMENT ON COLUMN t_booking.version IS 'Optimistic locking version field managed by JPA';
