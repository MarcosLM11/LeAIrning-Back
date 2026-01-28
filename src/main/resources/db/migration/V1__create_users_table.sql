-- V1: Initial schema creation for users table with auditing fields
CREATE TABLE users (
    id UUID PRIMARY KEY,
    password VARCHAR(255),
    email VARCHAR(255),
    version BIGINT,
    created_timestamp TIMESTAMP WITHOUT TIME ZONE,
    last_updated_timestamp TIMESTAMP WITHOUT TIME ZONE
);

-- Indexes for performance
CREATE INDEX idx_users_email ON users (email);

-- Documentation comments
COMMENT ON TABLE users IS 'User accounts with auditing capabilities';
COMMENT ON COLUMN users.version IS 'Optimistic locking version';
COMMENT ON COLUMN users.created_timestamp IS 'Record creation timestamp';
COMMENT ON COLUMN users.last_updated_timestamp IS 'Record last update timestamp';