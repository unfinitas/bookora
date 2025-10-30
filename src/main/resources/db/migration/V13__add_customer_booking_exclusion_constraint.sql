-- Prevent customer from booking multiple overlapping appointments
ALTER TABLE t_booking
ADD CONSTRAINT no_overlapping_customer_bookings
EXCLUDE USING gist (
    customer_id WITH =,
    tsrange(start_time, end_time, '[)') WITH &&
)
WHERE (status IN ('PENDING', 'CONFIRMED'));
