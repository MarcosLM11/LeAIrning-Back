-- V8: Add provider column to users table for OAuth2 provider tracking
ALTER TABLE users ADD COLUMN provider VARCHAR(50) DEFAULT 'local';

-- Update existing OAuth2 users (those without password) to 'google'
-- This assumes all existing OAuth2 users came from Google integration
UPDATE users SET provider = 'google' WHERE password IS NULL AND provider = 'local';

-- Make provider column non-nullable after setting defaults
ALTER TABLE users ALTER COLUMN provider SET NOT NULL;

-- Create composite index for efficient lookups by email and provider
CREATE INDEX idx_users_email_provider ON users (email, provider);

-- Documentation
COMMENT ON COLUMN users.provider IS 'OAuth2 provider or local authentication: google, github, local';
