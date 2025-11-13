-- Add last_password_reset_email_sent_at column to t_user table
-- Used for rate limiting password reset requests at database level

ALTER TABLE t_user
ADD COLUMN last_password_reset_email_sent_at TIMESTAMP;

COMMENT ON COLUMN t_user.last_password_reset_email_sent_at IS 'Timestamp of last password reset email sent (for rate limiting)';
