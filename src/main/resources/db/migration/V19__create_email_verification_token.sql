-- Create email verification token table
-- Separate entity for transient verification workflow (follows GuestAccessToken pattern)
-- Stores temporary tokens for email verification process

CREATE TABLE t_email_verification_token (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    token UUID NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint with cascade delete
    -- When user is deleted, all their verification tokens are automatically removed
    CONSTRAINT fk_email_verification_token_user
        FOREIGN KEY (user_id)
        REFERENCES t_user(id)
        ON DELETE CASCADE
);

-- Index on user_id for efficient lookup of tokens by user
CREATE INDEX idx_email_verification_token_user_id ON t_email_verification_token(user_id);

-- Index on token for fast verification queries (unique constraint already creates an index)
-- This comment documents that the UNIQUE constraint on token provides the index

-- Index on expires_at for cleanup queries (optional, for future batch cleanup jobs)
CREATE INDEX idx_email_verification_token_expires_at ON t_email_verification_token(expires_at);

COMMENT ON COLUMN t_email_verification_token.expires_at IS 'Expiration timestamp (7 days from generation)';
COMMENT ON COLUMN t_email_verification_token.used_at IS 'Timestamp when token was used (NULL = unused, NOT NULL = verified)';
