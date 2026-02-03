-- V1: Initial schema creation for users table with auditing fields
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255),
    name VARCHAR(255),
    picture_url VARCHAR(2048),
    role VARCHAR(36),
    password VARCHAR(255),
    created_timestamp TIMESTAMP WITHOUT TIME ZONE,
    last_updated_timestamp TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT
);

-- Indexes for performance
CREATE INDEX idx_users_email ON users (email);

-- Documentation comments
COMMENT ON TABLE users IS 'User accounts with auditing capabilities';
COMMENT ON COLUMN users.version IS 'Optimistic locking version';
COMMENT ON COLUMN users.created_timestamp IS 'Record creation timestamp';
COMMENT ON COLUMN users.last_updated_timestamp IS 'Record last update timestamp';