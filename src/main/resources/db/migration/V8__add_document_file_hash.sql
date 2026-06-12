-- =====================================================================
-- DocuRural - Hash de integridad para documentos (HU-30)
-- =====================================================================
-- Añade el campo file_hash para almacenar el SHA-256 del binario cargado.
-- Se mantiene nullable por compatibilidad con documentos históricos.
-- =====================================================================

ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS file_hash VARCHAR(64) NULL;

