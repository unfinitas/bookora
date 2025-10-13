-- Create t_availability table
CREATE TABLE t_availability(
    id BIGSERIAL PRIMARY KEY,
    provider_id UUID NOT NULL,
    availability_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_availability_provider FOREIGN KEY (provider_id) REFERENCES t_provider(id) ON DELETE CASCADE
);

-- Create index on provider_id for filtering availabilities by provider
CREATE INDEX idx_availability_provider_id ON t_availability(provider_id);

-- Create index on availability_date for date-based queries
CREATE INDEX idx_availability_availability_date ON t_availability(availability_date);

-- Create index on is_available for filtering available availabilities
CREATE INDEX idx_availability_is_available ON t_availability(is_available);

-- Create composite index for provider and date (common query pattern)
CREATE INDEX idx_availability_provider_id_availability_date ON t_availability(provider_id, availability_date);

-- Create composite index for provider, date and availability
CREATE INDEX idx_availability_provider_id_availability_date_is_available ON t_availability(provider_id, availability_date, is_available);
