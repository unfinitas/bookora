-- Change unique constraints to partial indexes for soft delete support

-- t_user: email and username
ALTER TABLE t_user
DROP CONSTRAINT IF EXISTS uq_user_email;

ALTER TABLE t_user
DROP CONSTRAINT IF EXISTS uq_user_username;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_email_active ON t_user(email) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_username_active ON t_user(username) WHERE deleted_at IS NULL;

-- t_provider: user_id
ALTER TABLE t_provider
DROP CONSTRAINT IF EXISTS uq_provider_user_id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_provider_user_id_active ON t_provider(user_id) WHERE deleted_at IS NULL;

-- t_guest_access_token: token and booking_id
ALTER TABLE t_guest_access_token
DROP CONSTRAINT IF EXISTS uq_guest_access_token_token;

ALTER TABLE t_guest_access_token
DROP CONSTRAINT IF EXISTS uq_guest_access_token_booking_id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_guest_access_token_token_active ON t_guest_access_token(token) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_guest_access_token_booking_id_active ON t_guest_access_token(booking_id) WHERE deleted_at IS NULL;
