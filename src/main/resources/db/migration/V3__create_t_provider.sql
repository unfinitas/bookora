-- Create t_provider table
CREATE TABLE t_provider (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    address_id BIGINT,
    business_name VARCHAR(255) NOT NULL,
    description TEXT,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_provider_user_id UNIQUE (user_id),
    CONSTRAINT fk_provider_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_provider_address FOREIGN KEY (address_id) REFERENCES t_address(id) ON DELETE SET NULL
);

-- Create index on address_id for location-based queries
CREATE INDEX idx_provider_address_id ON t_provider(address_id);

-- Create index on is_verified for filtering verified providers
CREATE INDEX idx_provider_is_verified ON t_provider(is_verified);
