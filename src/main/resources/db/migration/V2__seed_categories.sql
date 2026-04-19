-- =====================================================================
-- DocuRural - Seed de categorias predefinidas (Sprint 1)
-- =====================================================================
-- Inserta las 8 categorias documentales predefinidas.
--
-- La operacion es idempotente: ON CONFLICT (name) DO NOTHING evita
-- duplicados si la migracion se reaplica o si las categorias ya
-- existen por otra via. created_by se deja NULL a proposito para no
-- acoplar el seed al usuario administrador inicial.
-- =====================================================================

INSERT INTO categories (name, description, status)
VALUES ('Actas', 'Actas de reuniones, consejos directivos, comites', 'ACTIVE') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status)
VALUES ('Resoluciones', 'Resoluciones rectorales y administrativas', 'ACTIVE') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status)
VALUES ('Matriculas', 'Documentos de inscripcion y registro de estudiantes', 'ACTIVE') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status)
VALUES ('Certificados', 'Constancias de estudio, certificados de notas, diplomas',
        'ACTIVE') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status)
VALUES ('Correspondencia', 'Comunicados oficiales enviados y recibidos', 'ACTIVE') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status)
VALUES ('Informes', 'Informes pedagogicos, academicos, de gestion y del programa de biotecnologia',
        'ACTIVE') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status)
VALUES ('Normatividad', 'Manuales de convivencia, PEI, planes de area, protocolos del laboratorio de biotecnologia',
        'ACTIVE') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status)
VALUES ('Otro', 'Documentos que no corresponden a ninguna categoria anterior', 'ACTIVE') ON CONFLICT (name) DO NOTHING;
