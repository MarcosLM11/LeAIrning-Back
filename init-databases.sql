-- Bases de datos para microservicios
CREATE DATABASE usersdb;
CREATE DATABASE documentsdb;
CREATE DATABASE authdb;

-- Opcional: usuarios específicos
CREATE USER users_service WITH ENCRYPTED PASSWORD 'users_pass';
CREATE USER documents_service WITH ENCRYPTED PASSWORD 'documents_pass';
CREATE USER auth_service WITH ENCRYPTED PASSWORD 'auth_pass';

GRANT ALL PRIVILEGES ON DATABASE usersdb TO users_service;
GRANT ALL PRIVILEGES ON DATABASE documentsdb TO documents_service;
GRANT ALL PRIVILEGES ON DATABASE authdb TO auth_service;
