-- V2: Add role column to users table for authorization
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

CREATE INDEX idx_users_role ON users(role);

ALTER TABLE users ADD CONSTRAINT chk_users_role
    CHECK (role IN ('USER', 'ADMIN'));

COMMENT ON COLUMN users.role IS 'User role for authorization (USER or ADMIN)';