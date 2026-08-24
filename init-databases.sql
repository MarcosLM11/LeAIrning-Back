-- BD
CREATE DATABASE leairningdb;
CREATE DATABASE authdb;

-- Users
CREATE USER leairning WITH ENCRYPTED PASSWORD 'leairning';
CREATE USER auth_service WITH ENCRYPTED PASSWORD 'auth_service';

-- Privileges
GRANT ALL PRIVILEGES ON DATABASE leairningdb TO leairning;
GRANT ALL PRIVILEGES ON DATABASE authdb TO auth_service;

-- PostgreSQL 15+ no concede CREATE sobre el schema "public" solo por ser
-- propietario de privilegios de la base; hay que ser owner de la base
-- (miembro de pg_database_owner) para que Flyway pueda crear tablas.
ALTER DATABASE leairningdb OWNER TO leairning;
ALTER DATABASE authdb OWNER TO auth_service;

