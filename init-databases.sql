-- BD
CREATE DATABASE leairningdb;

-- Users
CREATE USER leairning WITH ENCRYPTED PASSWORD 'leairning';

-- Privileges
GRANT ALL PRIVILEGES ON DATABASE leairningdb TO leairning;

