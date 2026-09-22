# Changelog

Registro de las versiones de **DocuRural Backend** desplegadas a producción.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto
se adhiere a [Versionado Semántico](https://semver.org/lang/es/).

La fecha de cada versión corresponde a su **paso a producción**.

---

## [Unreleased]

### Added

- Inicializado **Spec Kit** como flujo de trabajo de desarrollo: plantillas de especificación,
  plan, tareas y checklist, scripts de apoyo y los comandos de ciclo de vida de una funcionalidad
  (`/speckit-specify`, `/speckit-plan`, `/speckit-tasks`, `/speckit-implement`, entre otros).
- Añadida la **constitución del proyecto** (`.specify/memory/constitution.md` v1.0.0): nueve
  principios verificables sobre arquitectura modular por feature, inversión de dependencias,
  simplicidad, contrato de pruebas por capa, umbrales de cobertura, migraciones inmutables,
  borrado lógico, manejo tipificado de errores y auditoría, y seguridad por defecto.
- Añadido `CLAUDE.md` con las convenciones de detalle del proyecto: naming de clases, métodos,
  base de datos y claves i18n, niveles de log, patrón MapStruct, reparto CQRS del módulo
  `document` y plantillas de prueba.

- Añadido el **permiso para aprobar documentos** (HU-32): indicador `canApprove` en los usuarios,
  independiente del rol, que el administrador asigna al crear o editar y que se devuelve en las
  respuestas de creación, edición, detalle y listado. Los lectores no pueden tenerlo (se rechaza
  con 400 y se retira automáticamente al cambiar el rol a lector), los cambios se registran en la
  bitácora como `can_approve: a → b`, y `UserService.isActiveApprover` consulta el estado vigente
  en cada llamada. Incluye la migración `V3__add_can_approve_to_users.sql`.

### Fixed

- Acotada la longitud de `confirmPassword` en la edición de usuario: ahora se rechaza con 400 una
  confirmación de más de 128 caracteres.

### Removed

- Eliminados los documentos de reglas heredados (`.claude/rules/`, `docs/legacy-rules/`), cuyo
  contenido quedó repartido entre la constitución y `CLAUDE.md`.

## [1.0.0] - 2026-08-13

Primera versión productiva del sistema de gestión documental de la IERD Mina y Ticha. Cubre el
ciclo completo del archivo digital: los administrativos cargan y clasifican documentos
institucionales, y docentes y personal autorizado los consultan y descargan según su rol. Toda
acción sobre el sistema queda registrada para trazabilidad.

### Added

- Añadido sistema de **autenticación y control de acceso**: inicio y cierre de sesión con token
  JWT y tres roles (`ADMIN`, `EDITOR`, `READER`) que determinan qué puede hacer cada persona en
  el sistema.
- Añadida **gestión de usuarios**: alta, consulta, edición y activación/desactivación de cuentas
  por parte de administradores.
- Añadida taxonomía administrable de **categorías documentales**, con desactivación lógica y
  conteo de documentos asociados.
- Añadido el **ciclo de vida completo de documentos**: carga individual y por lote (hasta 5
  archivos), visualización en línea, descarga, edición de metadatos y eliminación lógica, con
  validación de tipo y tamaño de archivo.
- Añadido listado paginado con **búsqueda por texto y filtros combinables** (categoría, fechas,
  responsable, estado).
- Añadido **panel de control** con vista de resumen: totales del repositorio, distribución por
  categoría y últimos documentos cargados.
- Añadida **bitácora de auditoría** de todas las acciones relevantes (inicios de sesión, cargas,
  consultas, descargas, cambios y eliminaciones) con autor, fecha e IP.
- Añadidos **niveles de confidencialidad** con restricciones de acceso y huella SHA-256 por
  archivo para verificar que no fue alterado.
- Añadido **almacenamiento en la nube**: archivos en Amazon S3, con alternativa en disco local
  para desarrollo.

---