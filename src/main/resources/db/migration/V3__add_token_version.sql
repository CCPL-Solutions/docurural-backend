-- M-1: columna de versión de token para revocación inmediata de sesiones
-- Al incrementarse, los tokens anteriores quedan inválidos aunque su firma sea correcta.
ALTER TABLE users
    ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
