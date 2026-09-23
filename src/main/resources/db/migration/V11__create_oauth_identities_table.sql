-- V11: Links a local user to one or more social login identities (Google, GitHub)
CREATE TABLE oauth_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    created_timestamp TIMESTAMP WITHOUT TIME ZONE,
    last_updated_timestamp TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT
);

CREATE UNIQUE INDEX idx_oauth_identities_provider_provider_id ON oauth_identities (provider, provider_id);
CREATE INDEX idx_oauth_identities_user_id ON oauth_identities (user_id);

COMMENT ON TABLE oauth_identities IS 'Links a local user to a social login identity (provider + provider-side id)';