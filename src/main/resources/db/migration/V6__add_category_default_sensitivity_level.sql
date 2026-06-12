-- =====================================================================
-- DocuRural - Nivel de sensibilidad por defecto en categorías (HU-28B)
-- =====================================================================
-- Añade el campo default_sensitivity_level a la tabla categories.
-- Matrículas y Certificados se inicializan como RESTRICTED (datos de
-- menores de edad — Ley 1581/2012). El resto permanece en INTERNAL.
-- =====================================================================

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS default_sensitivity_level VARCHAR(20) NOT NULL DEFAULT 'INTERNAL';

ALTER TABLE categories
    DROP CONSTRAINT IF EXISTS ck_categories_default_sensitivity_level;

ALTER TABLE categories
    ADD CONSTRAINT ck_categories_default_sensitivity_level
        CHECK (default_sensitivity_level IN ('INTERNAL', 'RESTRICTED', 'CONFIDENTIAL'));

UPDATE categories
SET default_sensitivity_level = 'RESTRICTED'
WHERE name IN ('Matrículas', 'Certificados');

CREATE INDEX IF NOT EXISTS idx_categories_default_sensitivity
    ON categories (default_sensitivity_level);
