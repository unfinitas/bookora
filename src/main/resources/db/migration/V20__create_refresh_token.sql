-- Create refresh token table for secure authentication with token rotation
CREATE TABLE t_refresh_token (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    token_family UUID NOT NULL,  -- Token rotation chain tracking
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    replaced_by_token_id BIGINT,  -- ID of the new token that replaced this one
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
        REFERENCES t_user(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_refresh_token_replaced_by
        FOREIGN KEY (replaced_by_token_id)
        REFERENCES t_refresh_token(id)
        ON DELETE SET NULL
);

-- Create indexes for performance optimization
CREATE INDEX idx_refresh_token_user_id ON t_refresh_token(user_id);
CREATE INDEX idx_refresh_token_token_hash ON t_refresh_token(token_hash);
CREATE INDEX idx_refresh_token_token_family ON t_refresh_token(token_family);
CREATE INDEX idx_refresh_token_expires_at ON t_refresh_token(expires_at);
CREATE INDEX idx_refresh_token_revoked_at ON t_refresh_token(revoked_at) WHERE revoked_at IS NOT NULL;

-- Add comment for documentation
COMMENT ON TABLE t_refresh_token IS 'Stores refresh tokens with rotation support for secure authentication';
COMMENT ON COLUMN t_refresh_token.token_hash IS 'SHA-256 hash of the actual token for security';
COMMENT ON COLUMN t_refresh_token.token_family IS 'Groups related tokens for detecting reuse attacks';
COMMENT ON COLUMN t_refresh_token.replaced_by_token_id IS 'References the new token when rotated';