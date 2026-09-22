-- HU-32: permiso para aprobar documentos, independiente del rol.
ALTER TABLE users ADD COLUMN IF NOT EXISTS can_approve BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users DROP CONSTRAINT IF EXISTS ck_users_can_approve_not_reader;
ALTER TABLE users ADD CONSTRAINT ck_users_can_approve_not_reader
    CHECK (NOT (role = 'READER' AND can_approve));
