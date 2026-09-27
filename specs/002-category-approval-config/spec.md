# Feature Specification: Configuración de aprobación por categoría

**Feature Branch**: `feature/hu-31`

**Created**: 2026-09-26

**Status**: Draft

**Input**: User description: "HU-31 — Configuración de aprobación por categoría (RF-07, prioridad Alta, versión v2.0 Flujo de aprobación). Como administrador del sistema, quiero indicar qué categorías documentales requieren aprobación, para que solo los documentos que lo necesitan pasen por el flujo, sin agregar pasos a los demás."

## Clarifications

### Session 2026-09-26

- Q: ¿Esta historia entrega un método reutilizable para saber si una categoría requiere aprobación o solo guarda y expone el indicador? → A: Entrega `CategoryService.requiresApproval(categoryId)`, que lee el valor vigente de la base de datos, con pruebas unitarias.
- Q: Cuando una edición no envía el campo "Requiere aprobación", ¿se conserva el valor actual o se rechaza la solicitud? → A: El campo es opcional en la edición; si no se envía, se conserva el valor actual (sin error de validación).
- Q: Al crear una categoría, ¿el registro `CREATE_CATEGORY` debe indicar el valor inicial de "Requiere aprobación"? → A: Sí, siempre: el detalle añade el valor inicial, por ejemplo "Categoria creada: Actas (requires_approval: true)".

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Marcar una categoría como "Requiere aprobación" (Priority: P1)

Un administrador crea una categoría nueva o edita una existente y activa el interruptor
"Requiere aprobación" (Sí/No) para indicar que los documentos de esa categoría deben pasar por el
flujo de aprobación (por ejemplo, Actas y Resoluciones). En las categorías nuevas el interruptor
parte en No. Solo el administrador puede cambiar este valor.

**Why this priority**: es el núcleo de la historia. Sin este indicador, el flujo de aprobación
de la versión v2.0 no sabe a qué documentos aplicarse.

**Independent Test**: un administrador crea una categoría sin tocar el interruptor y verifica que
queda en No; luego la edita, activa el interruptor, guarda y verifica que queda en Sí.

**Acceptance Scenarios**:

1. **Given** un administrador en el formulario de creación de categoría, **When** no toca el
   interruptor y guarda, **Then** la categoría se crea con "Requiere aprobación" en No.
2. **Given** un administrador creando una categoría, **When** activa el interruptor y guarda,
   **Then** la categoría se crea con "Requiere aprobación" en Sí.
3. **Given** una categoría activa con "Requiere aprobación" en No, **When** un administrador la
   edita, activa el interruptor y guarda, **Then** queda en Sí.
4. **Given** un usuario con rol EDITOR o READER, **When** intenta crear o editar una categoría
   (incluido el indicador), **Then** el sistema responde con acceso denegado (HTTP 403) y el
   mensaje "No tiene permisos para realizar esta acción", sin guardar ningún cambio.
5. **Given** una solicitud de edición que no incluye el indicador, **When** se guarda, **Then**
   el valor actual de "Requiere aprobación" se conserva.

---

### User Story 2 - Aviso de alcance del cambio y trazabilidad (Priority: P1)

Cuando el administrador cambia el valor de "Requiere aprobación", el sistema le informa que el
cambio solo afecta a los documentos que se carguen desde ese momento y deja constancia del cambio
en la bitácora de actividad con el valor anterior y el nuevo. Los documentos existentes no se
modifican: si se desactiva la opción, los que ya estén en el flujo lo continúan normalmente.

**Why this priority**: evita que el administrador crea que el cambio es retroactivo y garantiza
la trazabilidad exigida para toda edición de categorías.

**Independent Test**: cambiar el indicador de una categoría con documentos existentes y
verificar que la respuesta incluye el aviso de alcance, que los documentos existentes no cambian
y que la bitácora registra `requires_approval: false → true`.

**Acceptance Scenarios**:

1. **Given** una categoría con "Requiere aprobación" en No, **When** el administrador la cambia
   a Sí y guarda, **Then** el sistema informa: "Este cambio solo afecta a los documentos que se
   carguen desde ahora. Los documentos existentes conservan su estado actual".
2. **Given** ese mismo guardado, **When** se consulta la bitácora, **Then** existe una entrada
   con la acción EDIT_CATEGORY cuyo detalle incluye `requires_approval: false → true`.
3. **Given** una edición que no cambia el valor del indicador, **When** se guarda, **Then** no
   se muestra el aviso de alcance y el detalle de la bitácora no incluye la línea
   `requires_approval`.
4. **Given** una categoría con documentos ya cargados, **When** el administrador cambia el
   indicador (en cualquier sentido), **Then** ningún documento existente cambia de estado.
5. **Given** una categoría con "Requiere aprobación" en Sí y documentos en Borrador o En
   revisión, **When** el administrador la cambia a No, **Then** esos documentos continúan su
   flujo hasta ser aprobados, devueltos o archivados.

---

### User Story 3 - Advertencia por pocos aprobadores (Priority: P2)

Al activar "Requiere aprobación", si el sistema tiene menos de dos usuarios activos con permiso
de aprobar, el administrador recibe una advertencia no bloqueante: "Hay menos de dos usuarios con
permiso de aprobar. Los documentos que cargue un aprobador no podrán ser aprobados por él mismo".
El guardado se completa igual.

**Why this priority**: previene documentos atascados sin nadie que pueda aprobarlos, pero no
impide la configuración: el administrador puede designar aprobadores después.

**Independent Test**: con un solo aprobador activo, activar el indicador en una categoría y
verificar que se guarda y que la respuesta incluye la advertencia; con dos o más aprobadores
activos, verificar que la advertencia no aparece.

**Acceptance Scenarios**:

1. **Given** menos de dos aprobadores activos (cero o uno), **When** el administrador activa
   "Requiere aprobación" al crear o editar una categoría, **Then** la categoría se guarda y la
   respuesta incluye la advertencia.
2. **Given** dos o más aprobadores activos, **When** el administrador activa el indicador,
   **Then** la categoría se guarda sin la advertencia.
3. **Given** menos de dos aprobadores activos, **When** el administrador desactiva el indicador
   o guarda sin cambiarlo, **Then** no se muestra la advertencia.
4. **Given** un usuario con el permiso de aprobar pero desactivado, o con rol READER, **When** se
   cuentan los aprobadores activos, **Then** ese usuario no se cuenta.

---

### User Story 4 - Consultar el indicador en el listado y en el detalle (Priority: P2)

El listado de categorías muestra la columna "Requiere aprobación" con Sí/No, y el detalle de cada
categoría incluye el mismo dato. Los editores pueden consultarlo, porque el formulario de carga
de documentos lo necesita para avisar que el documento pasará por aprobación (HU-33).

**Why this priority**: da visibilidad a la configuración y habilita la historia de carga con
aviso, pero la configuración funciona sin ella.

**Independent Test**: con categorías con y sin el indicador, consultar el listado y el detalle
como ADMIN y como EDITOR y verificar que cada categoría expone su valor.

**Acceptance Scenarios**:

1. **Given** categorías con y sin el indicador, **When** un administrador consulta el listado,
   **Then** cada categoría indica si requiere aprobación.
2. **Given** un usuario con rol EDITOR, **When** consulta el listado o el detalle de categorías,
   **Then** recibe el valor de "Requiere aprobación" de cada una.
3. **Given** una categoría recién creada o editada, **When** se revisa la respuesta del guardado,
   **Then** incluye el valor vigente del indicador.

---

### User Story 5 - Estado inicial de las categorías existentes (Priority: P3)

Al introducir la funcionalidad, las 8 categorías predefinidas quedan con "Requiere aprobación" en
No, de modo que ningún flujo cambia hasta que el administrador decida qué categorías activar.

**Why this priority**: garantiza que el despliegue no altere el comportamiento actual; es un
efecto único de la puesta en marcha.

**Independent Test**: tras aplicar la actualización sobre una base con las 8 categorías
predefinidas, consultar el listado y verificar que todas están en No.

**Acceptance Scenarios**:

1. **Given** las 8 categorías existentes antes de la actualización, **When** se aplica la
   actualización, **Then** todas quedan con "Requiere aprobación" en No.

---

### Edge Cases

- Editar una categoría inactiva: se mantiene la regla vigente de que no se puede editar; el
  indicador tampoco cambia.
- Activar el indicador en una edición que además cambia nombre o sensibilidad: el detalle de la
  bitácora incluye todos los cambios en una sola entrada, con la línea `requires_approval` entre
  ellos.
- Crear una categoría directamente con el indicador en Sí: aplica la advertencia de pocos
  aprobadores; no aplica el aviso de alcance porque no hay documentos previos.
- Cero aprobadores activos: se trata igual que uno (advertencia no bloqueante).
- Varias ediciones seguidas que alternan el valor: cada cambio queda registrado en su propia
  entrada de bitácora.
- Un documento que se carga después de guardar el cambio: la regla nueva aplica a ese documento
  (comportamiento de HU-33, fuera de alcance aquí).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST asociar a cada categoría un indicador "Requiere aprobación" (sí/no),
  desactivado por defecto.
- **FR-002**: La creación de categorías MUST aceptar el indicador; si la solicitud no lo incluye,
  la categoría se crea con el valor desactivado.
- **FR-003**: La edición de categorías MUST aceptar el indicador como campo opcional; si la
  solicitud no lo incluye, MUST conservarse el valor actual y MUST NOT devolverse error de
  validación, de modo que los clientes que aún no envían el campo sigan funcionando. Omitir el
  campo cuenta como "sin cambio" (sin aviso de alcance ni línea en la bitácora).
- **FR-004**: Solo un usuario con rol ADMIN MUST poder crear o editar categorías y, por tanto,
  modificar el indicador. Cualquier otro rol MUST recibir acceso denegado (HTTP 403) con el
  mensaje "No tiene permisos para realizar esta acción", sin guardar cambios.
- **FR-005**: Las respuestas de creación, edición, detalle y listado de categorías MUST incluir
  el valor del indicador.
- **FR-006**: Los usuarios con rol ADMIN y EDITOR MUST poder consultar el listado y el detalle de
  categorías con el valor del indicador.
- **FR-007**: Cuando una edición cambie el valor del indicador, la respuesta MUST incluir el aviso
  informativo "Este cambio solo afecta a los documentos que se carguen desde ahora. Los documentos
  existentes conservan su estado actual". Si el valor no cambia, MUST NOT incluirse.
- **FR-008**: Cambiar el indicador MUST NOT modificar el estado ni ningún otro dato de los
  documentos ya existentes en la categoría; los documentos que ya estén en el flujo de aprobación
  lo continúan con normalidad.
- **FR-009**: Cuando una creación o edición deje el indicador activado habiendo estado
  desactivado (o inexistente, en la creación), y haya menos de dos usuarios aprobadores activos,
  la respuesta MUST incluir la advertencia no bloqueante "Hay menos de dos usuarios con permiso de
  aprobar. Los documentos que cargue un aprobador no podrán ser aprobados por él mismo", y el
  guardado MUST completarse igualmente.
- **FR-010**: Un usuario MUST contarse como aprobador activo solo si tiene el permiso de aprobar,
  está activo y su rol no es READER (misma definición que HU-32).
- **FR-011**: Cada vez que el valor del indicador cambie en una edición, el sistema MUST registrar
  en la bitácora de actividad una entrada con la acción EDIT_CATEGORY cuyo detalle incluya
  `requires_approval: [valor anterior] → [valor nuevo]`. Si el valor no cambia, MUST NOT incluirse
  esa línea. Al crear una categoría, el detalle de la entrada `CREATE_CATEGORY` MUST incluir
  siempre el valor inicial, con el formato `Categoria creada: [nombre] (requires_approval: [valor])`.
- **FR-012**: Al introducir la funcionalidad, todas las categorías existentes MUST quedar con el
  indicador desactivado.
- **FR-013**: Los textos del aviso de alcance y de la advertencia MUST externalizarse como el
  resto de mensajes del sistema.
- **FR-014**: El sistema MUST ofrecer en la interfaz `CategoryService` el método
  `requiresApproval(categoryId)`, que consulta el valor vigente del indicador en la base de datos
  y lo devuelve. Si la categoría no existe, MUST responder con el error de recurso no encontrado
  habitual. La carga de documentos (HU-33, funcionalidad posterior) MUST usar este método en cada
  carga para decidir si el documento entra al flujo de aprobación, de modo que un cambio del
  indicador aplique desde la siguiente carga.

### Key Entities *(include if feature involves data)*

- **Categoría documental**: agrupación de documentos con nombre, descripción, estado y nivel de
  sensibilidad por defecto. Se añade el indicador "Requiere aprobación" (sí/no, por defecto no),
  que determina si los documentos cargados a partir de ese momento pasan por el flujo de
  aprobación.
- **Usuario aprobador activo**: usuario con permiso de aprobar (HU-32), activo y con rol distinto
  de READER. Esta historia solo los cuenta para decidir si mostrar la advertencia.
- **Entrada de bitácora de actividad**: para esta historia, las ediciones de categoría añaden al
  detalle el cambio del indicador con el formato `requires_approval: [anterior] → [nuevo]`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un administrador puede activar o desactivar la aprobación de una categoría en una
  sola edición, sin pasos adicionales.
- **SC-002**: El 100% de los intentos de modificar el indicador por usuarios que no son ADMIN son
  rechazados y no alteran ninguna categoría.
- **SC-003**: El 100% de los cambios del indicador quedan registrados en la bitácora con el valor
  anterior y el nuevo, y el 100% de las categorías creadas registran su valor inicial.
- **SC-004**: Ningún documento existente cambia de estado como consecuencia de cambiar el
  indicador de su categoría.
- **SC-005**: En el 100% de las activaciones con menos de dos aprobadores activos, el
  administrador recibe la advertencia y la categoría queda guardada.
- **SC-006**: Administradores y editores identifican qué categorías requieren aprobación desde el
  listado, sin abrir el detalle de cada una.
- **SC-007**: Tras la actualización, las 8 categorías predefinidas aparecen con "Requiere
  aprobación" en No.
- **SC-008**: Tras cambiar el indicador de una categoría, `requiresApproval` devuelve el valor
  nuevo en la siguiente consulta, sin esperas ni reinicios.

## Assumptions

- Los formularios de creación (HU-16) y edición (HU-17) de categorías, el listado (HU-19) y el
  permiso de aprobar (HU-32, con su verificación de aprobador activo) ya existen; esta historia
  los extiende.
- El flujo de aprobación de documentos (estados Borrador y En revisión, aprobar, devolver) y el
  aviso en la carga de documentos (HU-33) son posteriores y quedan fuera de alcance. Aquí se
  entrega la consulta `requiresApproval(categoryId)` que esa funcionalidad deberá usar y se
  garantiza que cambiar el indicador no toca documentos existentes, lo que asegura FR-008 cuando
  esos estados existan.
- La gestión de categorías ya es exclusiva del rol ADMIN y la consulta (listado y detalle) ya está
  abierta a ADMIN y EDITOR; se conserva ese esquema, que cubre la necesidad de HU-33. El rol
  READER sigue sin acceso a la gestión de categorías.
- La parte visual (interruptor Sí/No, columna del listado, presentación del aviso y la
  advertencia) es responsabilidad del cliente web; el backend guarda el valor, lo expone y entrega
  el aviso y la advertencia en la respuesta del guardado.
- El aviso de alcance se entrega solo en ediciones que cambian el valor; en la creación no aplica
  porque la categoría no tiene documentos previos.
- La advertencia de pocos aprobadores se evalúa solo cuando el indicador pasa a activado (en
  creación con Sí o en edición de No a Sí), no en ediciones que lo mantienen activado.
- La línea `requires_approval: anterior → nuevo` aplica solo a ediciones; la creación registra el
  valor inicial dentro del detalle de `CREATE_CATEGORY` (FR-011).
- Las reglas vigentes de edición (una categoría inactiva no se puede editar, el nombre debe ser
  único) se mantienen sin cambios.
- Los cambios de esta historia deben quedar en `CHANGELOG.md` como establece la constitución.
