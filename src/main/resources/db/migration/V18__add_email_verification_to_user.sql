-- Add email verification state fields to t_user table
-- Following separation of concerns: User entity stores persistent state,
-- EmailVerificationToken entity (V10) stores transient workflow data

ALTER TABLE t_user
    ADD COLUMN is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN last_verification_email_sent_at TIMESTAMP;

-- Auto-verify existing users (users created before email verification was implemented)
-- This ensures backward compatibility for pre-existing accounts
UPDATE t_user
SET is_email_verified = TRUE
WHERE created_at < NOW();

-- Add comments explaining the columns
COMMENT ON COLUMN t_user.is_email_verified IS 'Indicates if user has verified their email address';
COMMENT ON COLUMN t_user.last_verification_email_sent_at IS 'Timestamp of last verification email sent (used for rate limiting: 1 email per hour)';
