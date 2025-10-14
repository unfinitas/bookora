-- Create t_user table
CREATE TABLE t_user (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    is_guest BOOLEAN NOT NULL DEFAULT FALSE,
    phone_number VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_user_email UNIQUE (email),
    CONSTRAINT chk_user_password CHECK (
        (is_guest = TRUE AND password IS NULL) OR
        (is_guest = FALSE AND password IS NOT NULL)
    )
);

-- Create index on email for faster lookups
CREATE INDEX idx_user_email ON t_user(email);

-- Create index on role for filtering
CREATE INDEX idx_user_role ON t_user(role);
