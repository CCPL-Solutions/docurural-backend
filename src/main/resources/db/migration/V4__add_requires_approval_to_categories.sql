-- HU-31: indica qué categorías requieren aprobación; las existentes quedan en false.
ALTER TABLE categories ADD COLUMN IF NOT EXISTS requires_approval BOOLEAN NOT NULL DEFAULT FALSE;
