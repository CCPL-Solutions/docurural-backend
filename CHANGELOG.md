# Changelog

Registro de las versiones de **DocuRural Backend** desplegadas a producción.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto
se adhiere a [Versionado Semántico](https://semver.org/lang/es/).

La fecha de cada versión corresponde a su **paso a producción**.

## [Unreleased]

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
- Añadidos **pipelines de CI/CD** para los ambientes de desarrollo, QA y producción, y endpoints
  de salud y versión (`/api/actuator/health`, `/api/version`) para verificar cada despliegue.
- Añadida **sincronización automática con el tablero de GitHub Projects**: el issue padre de la
  release, declarado en `release-configuration.json`, avanza de estado solo en los puntos
  deterministas del pipeline (fin de despliegue a Desarrollo, inicio/fin de despliegue a QA,
  inicio/fin de despliegue a Producción y verificación aprobada), sin afectar las demás
  transiciones que siguen siendo manuales.
- Añadida **cascada de estado del issue padre a sus sub-issues**: cuando el padre avanza de estado
  —ya sea por el pipeline o porque alguien mueve la tarjeta a mano en el tablero— sus sub-issues
  (las HU técnicas de la release) se alinean automáticamente, vía un relay en AWS Lambda
  (`github-webhook-relay/`) para el caso manual.
- Añadido disparo manual (`workflow_dispatch`) de los pipelines de despliegue a Desarrollo, QA y
  Producción, para reponer el componente en un ambiente cuya infraestructura fue recreada con
  Terraform sin necesidad de un commit ni de avanzar la release. Un despliegue manual nunca mueve
  el tablero de GitHub Projects.

### Changed

- Movido el workflow de la cascada manual (`project-cascade.yml`) al repo `CCPL-Solutions/project-automation`,
  para que sus ejecuciones no se mezclen con los pipelines de CI/CD de este repo en la pestaña de Actions. El
  caso automático (disparado por el propio pipeline de despliegue) no cambia.
- En el pipeline de Producción, el despliegue manual mantiene la aprobación del Environment
  `production` pero omite la verificación manual post-despliegue (`verify-production`), para no
  dejar la reposición de infraestructura pausada esperando una segunda aprobación.
- Los tres jobs de despliegue (`_deploy.yml`) ahora usan `concurrency` por ambiente, para que dos
  ejecuciones al mismo ambiente (ej. un push y un disparo manual solapados) se encolen en vez de
  corromper el respaldo del JAR (`JAR_BACKUP_PATH`).

[Unreleased]: https://github.com/CCPL-Solutions/docurural-backend/compare/main...develop
