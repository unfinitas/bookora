-- Add soft delete columns to email verification token table
ALTER TABLE t_email_verification_token
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by VARCHAR(255);

-- Add soft delete columns to refresh token table
ALTER TABLE t_refresh_token
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by VARCHAR(255);

-- Add indexes on deleted_at for soft delete filtering (WHERE deleted_at IS NULL)
CREATE INDEX idx_email_verification_token_deleted_at ON t_email_verification_token(deleted_at);
CREATE INDEX idx_refresh_token_deleted_at ON t_refresh_token(deleted_at);


