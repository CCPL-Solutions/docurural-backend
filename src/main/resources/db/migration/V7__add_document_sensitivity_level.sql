-- =====================================================================
-- DocuRural - Nivel de sensibilidad en documentos (HU-28)
-- =====================================================================
-- Añade el campo sensitivity_level a la tabla documents.
-- Los documentos existentes heredan el default_sensitivity_level de su
-- categoría cuando esta es distinta de INTERNAL.
-- =====================================================================

ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS sensitivity_level VARCHAR(20) NOT NULL DEFAULT 'INTERNAL';

ALTER TABLE documents
    DROP CONSTRAINT IF EXISTS ck_documents_sensitivity_level;

ALTER TABLE documents
    ADD CONSTRAINT ck_documents_sensitivity_level
        CHECK (sensitivity_level IN ('INTERNAL', 'RESTRICTED', 'CONFIDENTIAL'));

UPDATE documents d
SET sensitivity_level = c.default_sensitivity_level
FROM categories c
WHERE d.category_id = c.id
  AND d.status = 'ACTIVE'
  AND c.default_sensitivity_level <> 'INTERNAL';

CREATE INDEX IF NOT EXISTS idx_documents_sensitivity_level
    ON documents (sensitivity_level);
