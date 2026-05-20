-- HU-21 SRC-01: índice para acelerar filtros por rango de fecha del documento (document_date).
CREATE INDEX IF NOT EXISTS idx_documents_document_date ON documents (document_date);
