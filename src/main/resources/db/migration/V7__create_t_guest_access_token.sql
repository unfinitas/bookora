-- Create t_guest_access_token table
CREATE TABLE t_guest_access_token (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    token UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_guest_access_token_token UNIQUE (token),
    CONSTRAINT uq_guest_access_token_booking_id UNIQUE (booking_id),
    CONSTRAINT fk_guest_access_token_booking FOREIGN KEY (booking_id) REFERENCES t_booking(id) ON DELETE CASCADE
);

-- Create index on token for fast token lookup
CREATE INDEX idx_guest_access_token_token ON t_guest_access_token(token);

-- Create index on expires_at for cleanup queries
CREATE INDEX idx_guest_access_token_expires_at ON t_guest_access_token(expires_at);

-- Create index on used_at for filtering unused tokens
CREATE INDEX idx_guest_access_token_used_at ON t_guest_access_token(used_at);

-- Create composite index for token validation (token, used_at, expires_at)
CREATE INDEX idx_guest_access_token_validation ON t_guest_access_token(token, used_at, expires_at);
