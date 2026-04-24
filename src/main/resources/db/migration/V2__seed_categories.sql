-- =====================================================================
-- DocuRural - Seed de categorías predefinidas (Sprint 1)
-- =====================================================================
-- Inserta las 8 categorías documentales predefinidas.
--
-- La operación es idempotente: ON CONFLICT (name) DO NOTHING evita
-- duplicados si la migración se reaplica o si las categorías ya
-- existen por otra vía. created_by se deja NULL a propósito para no
-- acoplar el seed al usuario administrador inicial.
-- =====================================================================

INSERT INTO categories (name, description, status)
VALUES ('Actas', 'Actas de reuniones, consejos directivos, comités', 'ACTIVE') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status)
VALUES ('Resoluciones', 'Resoluciones rectorales y administrativas', 'ACTIVE') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status)
VALUES ('Matrículas', 'Documentos de inscripción y registro de estudiantes', 'ACTIVE') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status)
VALUES ('Certificados', 'Constancias de estudio, certificados de notas, diplomas',
        'ACTIVE') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status)
VALUES ('Correspondencia', 'Comunicados oficiales enviados y recibidos', 'ACTIVE') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status)
VALUES ('Informes', 'Informes pedagógicos, académicos, de gestión y del programa de biotecnología',
        'ACTIVE') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status)
VALUES ('Normatividad', 'Manuales de convivencia, PEI, planes de área, protocolos del laboratorio de biotecnología',
        'ACTIVE') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status)
VALUES ('Otro', 'Documentos que no corresponden a ninguna categoría anterior', 'ACTIVE') ON CONFLICT (name) DO NOTHING;
