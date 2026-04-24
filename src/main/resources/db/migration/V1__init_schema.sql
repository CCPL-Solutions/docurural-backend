-- =====================================================================
-- DocuRural - Esquema inicial (Sprint 1)
-- =====================================================================
-- Crea las cuatro tablas base del MVP según docs/modelo-datos.md:
--   * users         : cuentas del sistema (autenticación + roles)
--   * categories    : taxonomía documental (8 categorías predefinidas)
--   * documents     : metadatos de documentos cargados
--   * activity_log  : pista de auditoría de acciones de usuarios
--
-- Convenciones:
--   * Borrado lógico (status) en users, categories y documents.
--   * Enums modelados como VARCHAR + CHECK para mantener legibilidad.
--   * created_by en categories admite NULL para soportar el seed
--     de categorías sin acoplarlo a la creación del admin inicial.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Tabla: users
-- ---------------------------------------------------------------------
CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_login    TIMESTAMP NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'EDITOR', 'READER')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_status ON users (status);

-- ---------------------------------------------------------------------
-- Tabla: categories
-- ---------------------------------------------------------------------
CREATE TABLE categories
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  BIGINT NULL,
    CONSTRAINT uq_categories_name UNIQUE (name),
    CONSTRAINT ck_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT fk_categories_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE INDEX idx_categories_name ON categories (name);
CREATE INDEX idx_categories_status ON categories (status);

-- ---------------------------------------------------------------------
-- Tabla: documents
-- ---------------------------------------------------------------------
CREATE TABLE documents
(
    id                 BIGSERIAL PRIMARY KEY,
    title              VARCHAR(255) NOT NULL,
    description        TEXT NULL,
    category_id        BIGINT       NOT NULL,
    responsible_area   VARCHAR(100) NOT NULL,
    document_date      DATE         NOT NULL,
    file_path          VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_format        VARCHAR(20)  NOT NULL,
    file_size_bytes    BIGINT       NOT NULL,
    uploaded_by        BIGINT       NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT ck_documents_status CHECK (status IN ('ACTIVE', 'DELETED')),
    CONSTRAINT ck_documents_file_format CHECK (file_format IN ('PDF', 'DOCX', 'XLSX', 'JPG', 'PNG')),
    CONSTRAINT fk_documents_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_documents_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id)
);

CREATE INDEX idx_documents_category_id ON documents (category_id);
CREATE INDEX idx_documents_responsible_area ON documents (responsible_area);
CREATE INDEX idx_documents_uploaded_by ON documents (uploaded_by);
CREATE INDEX idx_documents_created_at ON documents (created_at);
CREATE INDEX idx_documents_status ON documents (status);

-- TODO Sprint 3: índice GIN/pg_trgm sobre documents.title y documents.description
-- para soportar búsqueda full-text rápida (RF-03). Se difiere del Sprint 1
-- porque la funcionalidad de búsqueda no entra en el alcance actual.

-- ---------------------------------------------------------------------
-- Tabla: activity_log
-- ---------------------------------------------------------------------
CREATE TABLE activity_log
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL,
    action           VARCHAR(50) NOT NULL,
    document_id      BIGINT NULL,
    action_timestamp TIMESTAMP   NOT NULL DEFAULT NOW(),
    ip_address       VARCHAR(45) NULL,
    detail           TEXT NULL,
    CONSTRAINT fk_activity_log_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_activity_log_document FOREIGN KEY (document_id) REFERENCES documents (id)
);

CREATE INDEX idx_activity_log_user_id ON activity_log (user_id);
CREATE INDEX idx_activity_log_document_id ON activity_log (document_id);
CREATE INDEX idx_activity_log_action_timestamp ON activity_log (action_timestamp);
CREATE INDEX idx_activity_log_action ON activity_log (action);
