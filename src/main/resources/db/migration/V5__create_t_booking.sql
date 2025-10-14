-- Create t_booking table
CREATE TABLE t_booking (
    id BIGSERIAL PRIMARY KEY,
    customer_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    service_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id) REFERENCES t_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_provider FOREIGN KEY (provider_id) REFERENCES t_provider(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_service FOREIGN KEY (service_id) REFERENCES t_service(id) ON DELETE CASCADE
);

-- Create index on customer_id for user's booking history
CREATE INDEX idx_booking_customer_id ON t_booking(customer_id);

-- Create index on provider_id for provider's bookings
CREATE INDEX idx_booking_provider_id ON t_booking(provider_id);

-- Create index on service_id
CREATE INDEX idx_booking_service_id ON t_booking(service_id);

-- Create index on status for filtering by booking status
CREATE INDEX idx_booking_status ON t_booking(status);

-- Create index on start_time for time-based queries
CREATE INDEX idx_booking_start_time ON t_booking(start_time);

-- Create composite index for provider and start_time (common query pattern)
CREATE INDEX idx_booking_provider_id_start_time ON t_booking(provider_id, start_time);
