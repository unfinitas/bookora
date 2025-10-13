-- Create t_address table
CREATE TABLE t_address (
    id BIGSERIAL PRIMARY KEY,
    street VARCHAR(255),
    city VARCHAR(255) NOT NULL,
    state_province VARCHAR(255),
    zip_code VARCHAR(20),
    country VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create index on city for location-based queries
CREATE INDEX idx_address_city ON t_address(city);

-- Create index on country for location-based queries
CREATE INDEX idx_address_country ON t_address(country);

-- Create composite index for city and country combination
CREATE INDEX idx_address_city_country ON t_address(city, country);
