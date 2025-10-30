-- Add temporal range exclusion constraint to prevent overlapping bookings
-- This prevents race conditions at the database level
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE t_booking
ADD CONSTRAINT no_overlapping_bookings
EXCLUDE USING gist (
    provider_id WITH =,
    tsrange(start_time, end_time, '[)') WITH &&
)
WHERE (status IN ('PENDING', 'CONFIRMED'));

