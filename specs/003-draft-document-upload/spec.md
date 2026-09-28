# Feature Specification: Carga de documento en estado Borrador

**Feature Branch**: `feature/hu-33`

**Created**: 2026-09-27

**Status**: Draft

**Input**: User description: "Se requiere implementar la HU-33: Carga de documento en estado Borrador descrita en @docs/DocuRural_HU_v2.0_FlujoAprobacion_v1_0.md. Ten en cuenta la info completa del documento pero el foco es la HU-33."

## Clarifications

### Session 2026-09-27

- Q: ¿Los documentos activos existentes deben pasar ya a Aprobado (D-08) en esta historia, o
  quedar Sin flujo y diferir la migración a T-03? → A: Pasan a APPROVED ya, en la misma
  migración que añade el estado del flujo. El evento MIGRATED se generará con la tabla de eventos
  (T-03) para los documentos APPROVED sin eventos, lo que no es ambiguo porque nadie puede aprobar
  hasta HU-37.
- Q: Mientras no exista HU-42, ¿quién ve los documentos en Borrador? → A: Siguen las reglas
  actuales (solo sensibilidad de HU-29). La rama de la v2.0 no se despliega a producción hasta
  completar HU-42; queda como condición de despliegue en Supuestos.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Carga individual en una categoría con aprobación queda en Borrador (Priority: P1)

Un administrador o editor carga un documento en una categoría marcada como "Requiere
aprobación" (HU-31), por ejemplo Actas. El sistema guarda el documento en estado Borrador para
que el autor pueda completarlo o corregirlo antes de enviarlo a revisión (HU-35). La confirmación
indica expresamente que el documento quedó como borrador.

**Why this priority**: es el punto de entrada de todo el flujo de aprobación (RF-07). Sin un
estado inicial Borrador no hay nada que enviar a revisión, aprobar ni devolver.

**Independent Test**: marcar una categoría como "Requiere aprobación", cargar un documento en
ella y verificar que queda en Borrador, que la confirmación dice "Documento cargado como
borrador" y que la bitácora registra la carga con `workflow_status: DRAFT`.

**Acceptance Scenarios**:

1. **Given** una categoría activa con "Requiere aprobación" en Sí, **When** un ADMIN o EDITOR
   carga un documento válido en ella, **Then** el documento se crea con estado del flujo
   Borrador (DRAFT).
2. **Given** esa misma carga, **When** se revisa la respuesta, **Then** incluye el estado del
   flujo resultante (DRAFT) y el mensaje "Documento cargado como borrador".
3. **Given** esa misma carga, **When** se consulta la bitácora de actividad, **Then** existe una
   entrada con la acción UPLOAD cuyo detalle incluye `workflow_status: DRAFT`.
4. **Given** esa misma carga, **When** se revisa el documento creado, **Then** tiene su huella
   de integridad SHA-256 calculada exactamente como en cualquier otra carga (HU-30).
5. **Given** una solicitud de carga que intenta indicar el estado del flujo, **When** se procesa,
   **Then** el sistema ignora ese valor y fija el estado únicamente a partir de la categoría.

---

### User Story 2 - Carga individual en una categoría sin aprobación conserva el comportamiento del MVP (Priority: P1)

Un administrador o editor carga un documento en una categoría que no requiere aprobación. El
documento queda "Sin flujo" (NOT_REQUIRED) y todo funciona igual que en el MVP: mismo mensaje de
confirmación y mismas reglas posteriores.

**Why this priority**: todas las categorías existentes parten sin flujo (HU-31); si esta ruta se
rompe, se rompe la carga del día a día de la institución.

**Independent Test**: cargar un documento en una categoría sin "Requiere aprobación" y verificar
que queda Sin flujo, que el mensaje es "Documento cargado exitosamente" y que la bitácora
registra `workflow_status: NOT_REQUIRED`.

**Acceptance Scenarios**:

1. **Given** una categoría activa con "Requiere aprobación" en No, **When** se carga un documento
   válido, **Then** el documento se crea con estado del flujo Sin flujo (NOT_REQUIRED).
2. **Given** esa carga, **When** se revisa la respuesta, **Then** incluye el estado NOT_REQUIRED y
   el mensaje habitual "Documento cargado exitosamente".
3. **Given** esa carga, **When** se consulta la bitácora, **Then** la entrada UPLOAD incluye en el
   detalle `workflow_status: NOT_REQUIRED`.
4. **Given** esa carga, **When** se aplican las validaciones existentes (formato, tamaño, tipo
   real, sensibilidad, permisos por rol), **Then** se comportan exactamente igual que antes.

---

### User Story 3 - Carga múltiple asigna el estado por documento según la categoría (Priority: P2)

En la carga múltiple (HU-10, hasta 5 archivos), cada documento cargado con éxito recibe el
estado del flujo que le corresponde por su categoría: si la categoría requiere aprobación, todos
quedan en Borrador. El resultado de cada archivo indica el estado resultante.

**Why this priority**: la carga múltiple es una vía alternativa de entrada; sin esta regla sería
un atajo para saltarse el flujo de aprobación.

**Independent Test**: realizar una carga múltiple de tres archivos en una categoría con
aprobación y verificar que los tres quedan en Borrador, que cada resultado exitoso lo indica y
que cada entrada UPLOAD de la bitácora incluye `workflow_status: DRAFT`.

**Acceptance Scenarios**:

1. **Given** una categoría con "Requiere aprobación" en Sí, **When** se cargan varios archivos
   válidos en un lote, **Then** todos los documentos creados quedan en Borrador.
2. **Given** una categoría sin aprobación, **When** se carga un lote, **Then** todos los
   documentos creados quedan Sin flujo.
3. **Given** un lote en una categoría con aprobación donde un archivo es inválido, **When** se
   procesa, **Then** los válidos quedan en Borrador, el inválido se reporta como fallido y no se
   crea documento para él.
4. **Given** un lote procesado, **When** se revisa el resultado de cada archivo exitoso, **Then**
   indica el estado del flujo con que quedó el documento.
5. **Given** un lote procesado, **When** se consulta la bitácora, **Then** cada documento creado
   tiene su propia entrada UPLOAD con el `workflow_status` correspondiente.

---

### User Story 4 - El estado del flujo no cambia al editar la categoría del documento (Priority: P2)

Una vez creado, el estado del flujo del documento queda fijo frente a cambios de categoría: si
después se mueve el documento a otra categoría (HU-13), o si el administrador cambia el
indicador "Requiere aprobación" de la categoría (HU-31), el estado del documento no se recalcula.

**Why this priority**: evita que un documento salte o salga del flujo por una edición de
metadatos, lo que permitiría eludir el control de aprobación.

**Independent Test**: cargar un documento en una categoría con aprobación (queda Borrador),
editar sus metadatos para moverlo a una categoría sin aprobación y verificar que sigue en
Borrador; y a la inversa, un documento Sin flujo movido a una categoría con aprobación sigue Sin
flujo.

**Acceptance Scenarios**:

1. **Given** un documento en Borrador, **When** se edita su categoría a una sin aprobación,
   **Then** el documento sigue en Borrador.
2. **Given** un documento Sin flujo, **When** se edita su categoría a una con aprobación,
   **Then** el documento sigue Sin flujo.
3. **Given** documentos ya cargados en una categoría, **When** el administrador cambia su
   indicador "Requiere aprobación", **Then** ningún documento existente cambia de estado; solo
   las cargas posteriores usan el valor nuevo.

---

### User Story 5 - Estado del flujo de los documentos existentes al desplegar (Priority: P3)

Al introducir el estado del flujo, los documentos que ya existen en el sistema reciben un estado
inicial coherente con la decisión de diseño D-08, sin fabricar aprobaciones que nadie dio.

**Why this priority**: es un efecto único de la puesta en marcha, pero condiciona cómo se
comportarán esos documentos en HU-40 (restricciones) y HU-42 (visibilidad).

**Independent Test**: aplicar la actualización sobre una base con documentos activos y
eliminados, y verificar el estado del flujo asignado a cada grupo.

**Acceptance Scenarios**:

1. **Given** documentos activos existentes antes de la actualización, **When** se aplica,
   **Then** quedan con el estado del flujo Aprobado (APPROVED), sin importar su categoría.
2. **Given** documentos eliminados lógicamente antes de la actualización, **When** se aplica,
   **Then** quedan con el estado del flujo Sin flujo (NOT_REQUIRED).
3. **Given** documentos existentes, **When** se aplica la actualización, **Then** su contador de
   ciclos de revisión queda en 0 y no se registra ningún aprobador ni visto bueno para ellos.

---

### Edge Cases

- Categoría inexistente o inactiva: se mantiene el rechazo actual (recurso no encontrado); no se
  crea documento ni se evalúa el flujo.
- La categoría cambia su indicador "Requiere aprobación" entre dos cargas: cada carga usa el
  valor vigente en el momento de guardar (lectura en cada carga, `requiresApproval` de HU-31).
- La categoría cambia su indicador en medio de un lote: los archivos del lote pueden quedar con
  estados distintos según el valor leído al guardar cada uno; es aceptable porque cada archivo se
  procesa de forma independiente.
- Falla el almacenamiento del archivo o la persistencia: no se crea documento ni entrada UPLOAD;
  el comportamiento de reversión actual no cambia.
- Falla el registro en la bitácora: la carga se completa igualmente (auditoría no intrusiva).
- No hay ningún aprobador activo en el sistema: la carga en Borrador se permite igual; la
  advertencia de pocos aprobadores ya se dio al configurar la categoría (HU-31).
- Un EDITOR carga en una categoría con aprobación un documento RESTRICTED o CONFIDENTIAL (cuando
  la categoría lo hereda): la carga queda en Borrador; las consecuencias de visibilidad
  corresponden a HU-42 y al punto abierto P-01.
- Un documento en Borrador eliminado lógicamente (HU-14): conserva su estado del flujo; la
  eliminación no lo modifica.
- READER intenta cargar: se mantiene el rechazo actual por rol (HTTP 403).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Todo documento MUST tener un estado del flujo con uno de estos valores: Sin flujo
  (NOT_REQUIRED), Borrador (DRAFT), En revisión (IN_REVIEW), Aprobado (APPROVED) o Archivado
  (ARCHIVED). Esta historia solo asigna NOT_REQUIRED y DRAFT en la carga; los demás valores
  quedan definidos para las historias HU-35 a HU-39.
- **FR-002**: Todo documento MUST tener un contador de ciclos de revisión, que en la carga MUST
  quedar en 0. Su incremento corresponde a HU-35.
- **FR-003**: Al cargar un documento (carga individual o múltiple), el sistema MUST fijar el
  estado del flujo en DRAFT si la categoría requiere aprobación en el momento de guardar, y en
  NOT_REQUIRED en caso contrario.
- **FR-004**: El sistema MUST determinar si la categoría requiere aprobación consultando el
  valor vigente a través de la consulta de solo lectura de categorías entregada en HU-31, en
  cada carga, de modo que un cambio del indicador aplique desde la carga siguiente.
- **FR-005**: El cliente MUST NOT poder indicar el estado del flujo en la solicitud de carga;
  cualquier valor enviado MUST ignorarse y el estado MUST derivarse solo de la categoría.
- **FR-006**: La respuesta de la carga individual MUST incluir el estado del flujo resultante.
  Si es DRAFT, el mensaje de confirmación MUST ser "Documento cargado como borrador"; si es
  NOT_REQUIRED, MUST conservarse el mensaje actual "Documento cargado exitosamente".
- **FR-007**: En la carga múltiple, el resultado de cada archivo cargado con éxito MUST incluir
  el estado del flujo con que quedó el documento. Los archivos fallidos no lo incluyen.
- **FR-008**: La entrada UPLOAD de la bitácora de actividad de cada documento creado MUST incluir
  en su detalle `workflow_status: DRAFT` o `workflow_status: NOT_REQUIRED`, además de la
  información que ya registra hoy (nombre del archivo).
- **FR-009**: El cálculo de la huella SHA-256 del archivo (HU-30), las validaciones de archivo
  (formato, tamaño, tipo real), las reglas de sensibilidad (HU-28/HU-29) y los permisos por rol
  de la carga MUST comportarse exactamente igual para ambos estados.
- **FR-010**: Editar los metadatos de un documento (HU-13), incluido su cambio de categoría, MUST
  NOT modificar su estado del flujo.
- **FR-011**: Cambiar el indicador "Requiere aprobación" de una categoría MUST NOT modificar el
  estado del flujo de los documentos ya existentes (coherente con HU-31, FR-008).
- **FR-012**: Al introducir la funcionalidad, en la misma actualización que añade el estado del
  flujo, los documentos activos existentes MUST quedar en APPROVED sin importar su categoría
  (decisión D-08) y los eliminados lógicamente en NOT_REQUIRED; todos con el contador de ciclos
  en 0. No se fabrica ningún aprobador ni visto bueno: el evento "Migración" (MIGRATED) de estos
  documentos se registrará cuando exista el historial de eventos del flujo (T-03), identificándolos
  como los documentos APPROVED sin eventos.
- **FR-013**: Mientras no exista HU-42, los documentos en Borrador MUST seguir las reglas de
  visibilidad vigentes (sensibilidad de HU-29), sin filtro adicional por estado del flujo. El
  filtrado por estado del flujo corresponde íntegramente a HU-42.
- **FR-014**: El nuevo mensaje de confirmación de carga en borrador MUST externalizarse como el
  resto de mensajes del sistema.
- **FR-015**: El estado del flujo MUST poder filtrarse eficientemente en consultas futuras
  (HU-36, HU-42): la búsqueda por estado no debe degradar la búsqueda por encima de los 3
  segundos en repositorios de hasta 10.000 documentos (RF-03).

### Key Entities *(include if feature involves data)*

- **Documento**: se añaden el estado del flujo (NOT_REQUIRED, DRAFT, IN_REVIEW, APPROVED,
  ARCHIVED; por defecto NOT_REQUIRED) y el contador de ciclos de revisión (por defecto 0). El
  estado del flujo es independiente del estado de vigencia actual (activo / eliminado).
- **Categoría documental**: aporta el indicador "Requiere aprobación" (HU-31), que se consulta en
  cada carga para decidir el estado inicial. No se modifica en esta historia.
- **Entrada de bitácora de actividad**: la acción UPLOAD añade al detalle el estado del flujo
  inicial con el formato `workflow_status: [valor]`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los documentos cargados (individual o en lote) en categorías con
  aprobación quedan en Borrador, y el 100% de los cargados en categorías sin aprobación quedan
  Sin flujo.
- **SC-002**: Ninguna solicitud de carga logra fijar un estado del flujo distinto del que
  corresponde a su categoría.
- **SC-003**: El autor identifica, desde la sola confirmación de la carga, si su documento quedó
  como borrador, sin abrir la ficha.
- **SC-004**: El 100% de las entradas UPLOAD de la bitácora creadas tras el despliegue indican el
  estado del flujo inicial del documento.
- **SC-005**: Las cargas en categorías sin aprobación no cambian en nada observable para el
  usuario respecto al MVP, salvo el nuevo dato del estado en la respuesta.
- **SC-006**: Tras cambiar el indicador de una categoría, la siguiente carga en ella ya refleja
  el valor nuevo, sin esperas ni reinicios.
- **SC-007**: Ningún documento cambia de estado del flujo como consecuencia de editar su
  categoría o de cambiar el indicador de su categoría.
- **SC-008**: Tras la actualización, el 100% de los documentos activos preexistentes aparecen como
  Aprobados y ninguno tiene aprobador registrado.

## Assumptions

- HU-31 (indicador "Requiere aprobación" y consulta de solo lectura `requiresApproval`) y HU-32
  (permiso de aprobar) ya están implementadas y se reutilizan sin cambios.
- El aviso del formulario de carga al seleccionar una categoría con aprobación (criterio 1 de la
  HU) es responsabilidad del cliente web; el backend ya expone el indicador de cada categoría a
  ADMIN y EDITOR (HU-31). Esta historia no añade nada en backend para ese aviso.
- La carga múltiple se sirve hoy por su propio punto de entrada (`/documents/batch`) y no por el
  de la carga individual, como sugiere la nota técnica de la HU; ambas vías quedan cubiertas.
- En la carga múltiple solo se cambia el resultado por archivo; el resumen del lote (totales) se
  mantiene igual.
- La exposición del estado del flujo en la ficha, el listado y la búsqueda, con su etiqueta de
  color y su filtro, corresponde a HU-41 y HU-42 y queda fuera de alcance.
- El envío a revisión (HU-35), el reemplazo de archivo (HU-34), la aprobación y devolución
  (HU-37/HU-38), el archivo (HU-39), las restricciones de edición y eliminación por estado
  (HU-40) y la tabla de eventos del flujo quedan fuera de alcance.
- Condición de despliegue: como los borradores aún no se ocultan por estado del flujo (FR-013),
  la rama de la v2.0 MUST NOT desplegarse a producción antes de completar HU-42.
- Los documentos migrados a APPROVED quedan sujetos, cuando existan, a las restricciones de
  HU-40 y a la visibilidad de HU-42 como cualquier documento aprobado.
- La numeración de migraciones del documento de HU (V6–V9) es orientativa; se usa la siguiente
  versión libre del repositorio.
- Los cambios de esta historia deben quedar en `CHANGELOG.md` como establece la constitución.
