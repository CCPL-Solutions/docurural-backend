-- M-9: normalización de emails a minúsculas e índice funcional para unicidad
-- Garantiza que Foo@x.com y foo@x.com no coexistan y que el login sea case-insensitive.
UPDATE users SET email = LOWER(email) WHERE email != LOWER(email);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_lower ON users (LOWER(email));
