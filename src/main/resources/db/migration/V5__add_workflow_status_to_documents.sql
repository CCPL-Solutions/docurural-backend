-- HU-33: estado del flujo de aprobación de documentos; los activos existentes pasan a APPROVED (D-08).
ALTER TABLE documents ADD COLUMN IF NOT EXISTS workflow_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED';
ALTER TABLE documents ADD COLUMN IF NOT EXISTS cycle_number INT NOT NULL DEFAULT 0;

ALTER TABLE documents DROP CONSTRAINT IF EXISTS ck_documents_workflow_status;
ALTER TABLE documents ADD CONSTRAINT ck_documents_workflow_status
    CHECK (workflow_status IN ('NOT_REQUIRED', 'DRAFT', 'IN_REVIEW', 'APPROVED', 'ARCHIVED'));

ALTER TABLE documents DROP CONSTRAINT IF EXISTS ck_documents_cycle_number;
ALTER TABLE documents ADD CONSTRAINT ck_documents_cycle_number CHECK (cycle_number >= 0);

-- D-08: no se fabrica ninguna aprobación; el evento MIGRATED llega con la tabla de eventos (T-03).
UPDATE documents SET workflow_status = 'APPROVED' WHERE status = 'ACTIVE' AND workflow_status = 'NOT_REQUIRED';

CREATE INDEX IF NOT EXISTS idx_documents_workflow_status ON documents (workflow_status);
