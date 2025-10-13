-- Create t_service table
CREATE TABLE t_service (
    id BIGSERIAL PRIMARY KEY,
    provider_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    duration_minutes INTEGER NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_service_provider FOREIGN KEY (provider_id) REFERENCES t_provider(id) ON DELETE CASCADE
);

-- Create index on provider_id for filtering services by provider
CREATE INDEX idx_service_provider_id ON t_service(provider_id);

-- Create index on is_active for filtering active services
CREATE INDEX idx_service_is_active ON t_service(is_active);

-- Create composite index for provider and active status
CREATE INDEX idx_service_provider_id_is_active ON t_service(provider_id, is_active);
