# Changelog

Registro de las versiones de **DocuRural Backend** desplegadas a producción.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto
se adhiere a [Versionado Semántico](https://semver.org/lang/es/).

La fecha de cada versión corresponde a su **paso a producción**.

## [1.0.0] - 2026-08-21 (programada)

Primera versión productiva del sistema de gestión documental de la IERD Mina y Ticha. Cubre el
ciclo completo del archivo digital: los administrativos cargan y clasifican documentos
institucionales, y docentes y personal autorizado los consultan y descargan según su rol. Toda
acción sobre el sistema queda registrada para trazabilidad.

### Añadido

- **Autenticación y control de acceso.** Inicio y cierre de sesión con token JWT y tres roles
  (`ADMIN`, `EDITOR`, `READER`) que determinan qué puede hacer cada persona en el sistema.
- **Gestión de usuarios.** Alta, consulta, edición y activación/desactivación de cuentas por
  parte de administradores.
- **Categorías documentales.** Taxonomía administrable para clasificar el archivo, con
  desactivación lógica y conteo de documentos asociados.
- **Ciclo de vida de documentos.** Carga individual y por lote (hasta 5 archivos),
  visualización en línea, descarga, edición de metadatos y eliminación lógica, con validación
  de tipo y tamaño de archivo.
- **Búsqueda y filtrado.** Listado paginado con búsqueda por texto y filtros combinables
  (categoría, fechas, responsable, estado).
- **Panel de control.** Vista de resumen con totales del repositorio, distribución por
  categoría y últimos documentos cargados.
- **Registro de auditoría.** Bitácora de todas las acciones relevantes (inicios de sesión,
  cargas, consultas, descargas, cambios y eliminaciones) con autor, fecha e IP.
- **Clasificación por sensibilidad e integridad.** Niveles de confidencialidad con
  restricciones de acceso y huella SHA-256 por archivo para verificar que no fue alterado.
- **Almacenamiento en la nube.** Archivos en Amazon S3, con alternativa en disco local para
  desarrollo.
- **Operación y despliegue.** Pipelines de CI/CD para los ambientes de desarrollo, QA y
  producción, y endpoints de salud y versión (`/api/actuator/health`, `/api/version`) para
  verificar cada despliegue.

[Sin publicar]: https://github.com/CCPL-Solutions/docurural-backend/compare/v1.0.0...develop

[1.0.0]: https://github.com/CCPL-Solutions/docurural-backend/releases/tag/v1.0.0
