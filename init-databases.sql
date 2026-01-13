-- Bases de datos para microservicios
CREATE DATABASE usersdb;
CREATE DATABASE documentsdb;

-- Opcional: usuarios específicos
CREATE USER users_service WITH ENCRYPTED PASSWORD 'users_pass';
CREATE USER documents_service WITH ENCRYPTED PASSWORD 'documents_pass';

GRANT ALL PRIVILEGES ON DATABASE usersdb TO users_service;
GRANT ALL PRIVILEGES ON DATABASE documentsdb TO documents_service;
