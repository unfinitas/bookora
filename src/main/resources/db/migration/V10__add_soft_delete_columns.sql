-- Add soft delete columns to all tables

ALTER TABLE t_user
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by VARCHAR(255);

ALTER TABLE t_provider
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by VARCHAR(255);

ALTER TABLE t_service
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by VARCHAR(255);

ALTER TABLE t_booking
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by VARCHAR(255);

ALTER TABLE t_guest_access_token
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by VARCHAR(255);

ALTER TABLE t_address
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by VARCHAR(255);

ALTER TABLE t_availability
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by VARCHAR(255);

-- Create indexes on deleted_at for performance
CREATE INDEX idx_user_deleted_at ON t_user(deleted_at);
CREATE INDEX idx_provider_deleted_at ON t_provider(deleted_at);
CREATE INDEX idx_service_deleted_at ON t_service(deleted_at);
CREATE INDEX idx_booking_deleted_at ON t_booking(deleted_at);
CREATE INDEX idx_guest_access_token_deleted_at ON t_guest_access_token(deleted_at);
CREATE INDEX idx_address_deleted_at ON t_address(deleted_at);
CREATE INDEX idx_availability_deleted_at ON t_availability(deleted_at);
