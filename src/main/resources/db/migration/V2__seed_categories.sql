-- =====================================================================
-- DocuRural - Seed de categorías predefinidas
-- =====================================================================
-- Inserta las 8 categorías documentales predefinidas, cada una con su
-- nivel de sensibilidad por defecto. Matrículas y Certificados nacen
-- como RESTRICTED por contener datos de menores de edad
-- (Ley 1581/2012); el resto queda en INTERNAL.
--
-- La operación es idempotente: ON CONFLICT (name) DO NOTHING evita
-- duplicados si la migración se reaplica o si las categorías ya
-- existen por otra vía. created_by se deja NULL a propósito para no
-- acoplar el seed al usuario administrador inicial.
-- =====================================================================

INSERT INTO categories (name, description, status, default_sensitivity_level)
VALUES ('Actas', 'Actas de reuniones, consejos directivos, comités', 'ACTIVE',
        'INTERNAL') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status, default_sensitivity_level)
VALUES ('Resoluciones', 'Resoluciones rectorales y administrativas', 'ACTIVE',
        'INTERNAL') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status, default_sensitivity_level)
VALUES ('Matrículas', 'Documentos de inscripción y registro de estudiantes', 'ACTIVE',
        'RESTRICTED') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status, default_sensitivity_level)
VALUES ('Certificados', 'Constancias de estudio, certificados de notas, diplomas', 'ACTIVE',
        'RESTRICTED') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status, default_sensitivity_level)
VALUES ('Correspondencia', 'Comunicados oficiales enviados y recibidos', 'ACTIVE',
        'INTERNAL') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status, default_sensitivity_level)
VALUES ('Informes', 'Informes pedagógicos, académicos, de gestión y del programa de biotecnología', 'ACTIVE',
        'INTERNAL') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status, default_sensitivity_level)
VALUES ('Normatividad', 'Manuales de convivencia, PEI, planes de área, protocolos del laboratorio de biotecnología',
        'ACTIVE', 'INTERNAL') ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, status, default_sensitivity_level)
VALUES ('Otro', 'Documentos que no corresponden a ninguna categoría anterior', 'ACTIVE',
        'INTERNAL') ON CONFLICT (name) DO NOTHING;
