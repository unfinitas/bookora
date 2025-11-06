-- Drop existing constraints that don't filter by deleted_at
ALTER TABLE t_booking DROP CONSTRAINT IF EXISTS no_overlapping_bookings;
ALTER TABLE t_booking DROP CONSTRAINT IF EXISTS no_overlapping_customer_bookings;

-- Recreate provider booking overlap constraint with soft delete filter
ALTER TABLE t_booking
ADD CONSTRAINT no_overlapping_bookings
EXCLUDE USING gist (
    provider_id WITH =,
    tsrange(start_time, end_time, '[)') WITH &&
)
WHERE (status IN ('PENDING', 'CONFIRMED') AND deleted_at IS NULL);

-- Recreate customer booking overlap constraint with soft delete filter
ALTER TABLE t_booking
ADD CONSTRAINT no_overlapping_customer_bookings
EXCLUDE USING gist (
    customer_id WITH =,
    tsrange(start_time, end_time, '[)') WITH &&
)
WHERE (status IN ('PENDING', 'CONFIRMED') AND deleted_at IS NULL);

-- Create index on deleted_at for better performance of soft delete queries
CREATE INDEX IF NOT EXISTS idx_booking_deleted_at ON t_booking(deleted_at) WHERE deleted_at IS NULL;