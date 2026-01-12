-- V1: Initial schema creation for users table with auditing fields
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    email VARCHAR(255),
    version BIGINT,
    created_timestamp TIMESTAMP WITHOUT TIME ZONE,
    last_updated_timestamp TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_users_username UNIQUE (username)
);

-- Indexes for performance
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);

-- Documentation comments
COMMENT ON TABLE users IS 'User accounts with auditing capabilities';
 COMMENT ON COLUMN users.version IS 'Optimistic locking version';
 COMMENT ON COLUMN users.created_timestamp IS 'Record creation timestamp';
 COMMENT ON COLUMN users.last_updated_timestamp IS 'Record last update timestamp';