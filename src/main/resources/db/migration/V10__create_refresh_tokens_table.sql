-- V10: Refresh tokens are opaque, hashed, and persisted so sessions survive app restarts
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_timestamp TIMESTAMP WITHOUT TIME ZONE,
    last_updated_timestamp TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT
);

CREATE UNIQUE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

COMMENT ON TABLE refresh_tokens IS 'Opaque, hashed refresh tokens backing session continuity across restarts';