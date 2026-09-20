# Feature Specification: Permiso para aprobar documentos

**Feature Branch**: `feature/hu-32`

**Created**: 2026-09-20

**Status**: Draft

**Input**: User description: "HU-32 — Permiso para aprobar documentos (RF-07, prioridad Alta, versión v2.0 Flujo de aprobación). Como administrador del sistema, quiero indicar qué usuarios pueden revisar y aprobar documentos, para que la responsabilidad de aprobar recaiga solo en las personas autorizadas por la institución, como la rectoría o la coordinación."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Otorgar el permiso de aprobar al crear o editar un usuario (Priority: P1)

Un administrador crea un usuario nuevo o edita uno existente y marca "Puede aprobar documentos"
para que esa persona (por ejemplo, la rectoría o la coordinación) pueda revisar y aprobar
documentos. El permiso es independiente del rol: ser ADMIN no lo otorga, y un EDITOR puede
recibirlo sin ser ADMIN.

**Why this priority**: es el núcleo de la historia. Sin la capacidad de designar aprobadores, el
flujo de aprobación de la versión v2.0 no tiene a quién asignar la responsabilidad.

**Independent Test**: un administrador crea un usuario EDITOR con el permiso activado, consulta
el usuario y verifica que el permiso quedó guardado. Luego edita a un ADMIN sin marcar el
permiso y verifica que sigue sin él.

**Acceptance Scenarios**:

1. **Given** un administrador en el formulario de creación de usuario, **When** no toca la
   casilla, **Then** el usuario se crea sin el permiso de aprobar (valor por defecto: desactivado).
2. **Given** un administrador creando un usuario con rol EDITOR, **When** marca "Puede aprobar
   documentos" y guarda, **Then** el usuario queda con el permiso activo.
3. **Given** un usuario ADMIN sin el permiso, **When** se consulta su información, **Then**
   aparece sin el permiso: el rol ADMIN no lo otorga automáticamente.
4. **Given** un usuario con rol EDITOR sin el permiso, **When** un administrador lo edita y
   marca la casilla, **Then** el permiso queda activo y el cambio se registra en la bitácora.
5. **Given** un usuario sin rol ADMIN, **When** intenta crear o modificar un usuario (incluida la
   casilla), **Then** el sistema rechaza la operación por falta de autorización.

---

### User Story 2 - Los lectores no pueden ser aprobadores (Priority: P1)

El permiso solo tiene sentido para quien puede escribir en el sistema. Un usuario con rol READER
no puede recibirlo: la casilla aparece deshabilitada con el texto "Los lectores no pueden aprobar
documentos", y el sistema lo impide aunque se intente por otra vía.

**Why this priority**: es la regla de integridad que protege la asignación de responsabilidad.
Sin ella se podría designar como aprobador a alguien que no puede intervenir en documentos.

**Independent Test**: intentar crear o editar un usuario READER con el permiso activo y verificar
que el sistema lo rechaza con un mensaje claro.

**Acceptance Scenarios**:

1. **Given** un formulario con rol READER seleccionado, **When** el administrador observa la
   casilla, **Then** está deshabilitada y muestra "Los lectores no pueden aprobar documentos".
2. **Given** una solicitud de creación o edición para un usuario READER con el permiso activo,
   **When** el sistema la procesa, **Then** la rechaza con un error de regla de negocio y no
   guarda ningún cambio.
3. **Given** un usuario ADMIN o EDITOR, **When** un administrador activa el permiso, **Then** el
   sistema lo acepta.

---

### User Story 3 - Retiro automático del permiso al pasar a lector (Priority: P2)

Cuando un administrador cambia el rol de un aprobador a READER, el sistema le retira el permiso
automáticamente, para que nunca exista un lector con capacidad de aprobar. El administrador es
informado de este efecto antes de confirmar el guardado.

**Why this priority**: evita estados inconsistentes derivados de cambios de rol. Es menos
frecuente que la asignación, pero necesaria para mantener la regla de la historia 2.

**Independent Test**: editar un EDITOR con el permiso cambiando su rol a READER y verificar que,
tras guardar, el usuario queda como READER sin el permiso y que el cambio consta en la bitácora.

**Acceptance Scenarios**:

1. **Given** un usuario EDITOR con el permiso, **When** un administrador cambia su rol a READER
   y guarda, **Then** el usuario queda como READER con el permiso retirado.
2. **Given** ese mismo cambio de rol, **When** el administrador está por guardar, **Then** el
   sistema le informa antes de confirmar que el permiso será retirado.
3. **Given** el retiro automático, **When** se guarda, **Then** la bitácora registra el cambio
   del permiso (de activo a inactivo) junto con el resto de la edición.

---

### User Story 4 - Ver quiénes son aprobadores en el listado (Priority: P2)

El administrador ve en el listado de usuarios una etiqueta "Aprobador" junto a cada usuario que
tiene el permiso, para saber de un vistazo quién puede aprobar documentos.

**Why this priority**: da visibilidad a la designación, pero el permiso funciona sin ella.

**Independent Test**: listar usuarios con al menos un aprobador y un no aprobador, y verificar
que solo el primero lleva la etiqueta.

**Acceptance Scenarios**:

1. **Given** usuarios con y sin el permiso, **When** el administrador consulta el listado,
   **Then** cada usuario indica si tiene el permiso y la etiqueta "Aprobador" se muestra solo en
   quienes lo tienen.

---

### User Story 5 - Efecto de desactivar, reactivar y retirar el permiso (Priority: P2)

Un usuario desactivado no puede iniciar sesión, así que no cuenta como aprobador activo; al
reactivarlo recupera el permiso que tenía. Retirar el permiso a alguien no borra los vistos
buenos que ya dio: siguen en el historial de cada documento. Además, retirar el permiso surte
efecto de inmediato, aunque la persona tenga una sesión abierta.

**Why this priority**: define el comportamiento en los casos límite del ciclo de vida del
usuario y protege la trazabilidad y la seguridad de la asignación.

**Independent Test**: desactivar a un aprobador y verificar que no cuenta como aprobador activo;
reactivarlo y verificar que conserva el permiso. Retirar el permiso a alguien con sesión vigente
y verificar que su siguiente intento de aprobar o devolver es rechazado.

**Acceptance Scenarios**:

1. **Given** un aprobador, **When** un administrador lo desactiva, **Then** conserva el valor
   guardado del permiso, pero no cuenta como aprobador activo.
2. **Given** un aprobador desactivado, **When** un administrador lo reactiva, **Then** vuelve a
   tener el permiso con el mismo valor que tenía.
3. **Given** un usuario con sesión vigente y con el permiso, **When** un administrador se lo
   retira, **Then** su siguiente intento de aprobar o devolver un documento es rechazado sin
   esperar a que su sesión expire.
4. **Given** un documento con vistos buenos de un usuario, **When** a ese usuario se le retira el
   permiso, **Then** sus vistos buenos previos permanecen intactos en el historial del documento.

---

### Edge Cases

- Un ADMIN que se quita el permiso a sí mismo: puede seguir administrando el sistema pero deja de
  poder aprobar.
- Cambiar el rol de un aprobador a READER y, en la misma solicitud, enviar el permiso activo: el
  retiro automático prevalece y el usuario queda como READER sin el permiso, sin error. (Ver
  Assumptions.)
- Un READER con el permiso ya en `false` que se edita sin tocar la casilla: no debe generarse
  ningún registro de cambio del permiso.
- Guardar una edición sin cambiar el valor del permiso: la bitácora no incluye la línea del
  permiso.
- Usuarios existentes antes de esta funcionalidad: todos quedan sin el permiso.
- Una solicitud de edición que omita el campo del permiso: no debe modificar el valor actual.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST asociar a cada usuario un indicador "puede aprobar documentos",
  desactivado por defecto, incluido en los usuarios ya existentes al introducir la funcionalidad.
- **FR-002**: Las operaciones de creación y edición de usuarios MUST aceptar ese indicador; si la
  creación no lo incluye, el usuario se crea con el valor desactivado.
- **FR-003**: Solo un usuario con rol ADMIN MUST poder crear o modificar usuarios y, por tanto,
  modificar el indicador. El rol ADMIN MUST NOT otorgar el permiso implícitamente: un ADMIN sin el
  indicador activo no puede aprobar documentos.
- **FR-004**: El sistema MUST permitir activar el indicador únicamente en usuarios con rol ADMIN o
  EDITOR, y MUST rechazar con un error de regla de negocio (mensaje traducible) cualquier
  solicitud que lo active en un usuario READER, sin guardar ningún cambio.
- **FR-005**: Cuando la edición cambie el rol de un usuario con el permiso a READER, el sistema
  MUST retirarle el permiso automáticamente en el mismo guardado.
- **FR-006**: La interfaz de administración MUST informar al administrador, antes de guardar, que
  el cambio de rol a READER retirará el permiso. La casilla MUST mostrarse deshabilitada con el
  texto "Los lectores no pueden aprobar documentos" cuando el rol seleccionado sea READER.
- **FR-007**: El sistema MUST NOT considerar aprobador activo a un usuario desactivado, y MUST
  conservar el valor del indicador durante la desactivación para restituirlo al reactivar.
- **FR-008**: Las respuestas de creación, edición y consulta/listado de usuarios MUST exponer el
  valor del indicador, de modo que el listado pueda mostrar la etiqueta "Aprobador" en los
  usuarios que lo tienen.
- **FR-009**: Retirar el permiso MUST NOT alterar ni eliminar los vistos buenos ya registrados en
  el historial de cada documento.
- **FR-010**: Cada vez que el valor del indicador cambie (por edición del administrador o por el
  retiro automático de FR-005), el sistema MUST registrar en la bitácora de actividad una entrada
  de acción `EDIT_USER` cuyo detalle incluya `can_approve: [valor anterior] → [valor nuevo]`. Si
  el valor no cambia, MUST NOT registrarse esa línea.
- **FR-011**: El permiso MUST NOT viajar como dato fijo dentro de la credencial de sesión (token).
  Cualquier acción que exija ser aprobador (aprobar o devolver un documento, funcionalidad
  posterior) MUST verificar el valor vigente del permiso y el estado activo del usuario en cada
  ocasión, de modo que retirarlo surta efecto de inmediato aunque el usuario tenga una sesión
  vigente.
- **FR-012**: Una solicitud de edición que no incluya el indicador MUST conservar el valor actual
  del usuario.

### Key Entities *(include if feature involves data)*

- **Usuario**: persona con acceso al sistema. Atributos relevantes: rol (ADMIN, EDITOR, READER),
  estado (activo/inactivo) y el nuevo indicador "puede aprobar documentos" (sí/no, por defecto no).
  El indicador es independiente del rol, aunque restringido por él (nunca activo en READER).
- **Entrada de bitácora de actividad**: registro de quién hizo qué. Para esta historia, las
  ediciones de usuario añaden al detalle el cambio del permiso en el formato
  `can_approve: [anterior] → [nuevo]`.
- **Visto bueno (fuera de alcance)**: registro de aprobación de un documento, creado por la
  funcionalidad de aprobación posterior. Esta historia solo garantiza que retirar el permiso no lo
  afecta.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un administrador puede designar a un usuario como aprobador, o retirarle el permiso,
  en una sola edición del usuario, sin pasos adicionales.
- **SC-002**: El 100% de los intentos de asignar el permiso a un lector son rechazados y ningún
  lector aparece con el permiso activo en el sistema.
- **SC-003**: El 100% de los cambios del permiso (manuales o automáticos) quedan registrados en la
  bitácora con el valor anterior y el nuevo.
- **SC-004**: Tras retirar el permiso, el usuario afectado no puede aprobar ni devolver documentos
  en su siguiente intento, incluso con una sesión abierta antes del retiro.
- **SC-005**: En el listado de usuarios, el administrador identifica quiénes son aprobadores sin
  abrir el detalle de cada usuario.
- **SC-006**: Ningún visto bueno registrado desaparece ni cambia como consecuencia de retirar el
  permiso a su autor.

## Assumptions

- Los formularios de creación y edición de usuarios (HU-03, HU-04), la desactivación (HU-05) y el
  listado (HU-08) ya existen; esta historia los extiende. La funcionalidad de aprobar y devolver
  documentos es posterior y queda fuera de alcance: aquí solo se define el permiso y la regla de
  verificación que esa funcionalidad deberá respetar.
- La parte visual (casilla, texto de ayuda, aviso previo al guardado, etiqueta "Aprobador") es
  responsabilidad del cliente web. El backend garantiza las reglas, expone el valor del permiso y
  rechaza los estados inválidos; el aviso previo se apoya en que el cliente conoce el rol
  seleccionado y el permiso actual del usuario.
- El retiro automático por cambio a READER se aplica en el servidor aunque el cliente envíe el
  permiso activo en esa misma solicitud: prevalece la regla de READER y no se devuelve error. La
  solicitud que *crea* un READER con el permiso activo sí se rechaza (FR-004), porque no hay un
  cambio de rol que justifique la corrección silenciosa.
- La condición de "aprobador activo" es un cálculo (permiso activo + usuario activo + rol distinto
  de READER), no un valor almacenado adicional.
- Los usuarios existentes al liberar la funcionalidad quedan sin el permiso; los administradores
  designan a los aprobadores manualmente.
- Los mensajes de error y de aviso se externalizan como el resto de textos del sistema.
- Los cambios de esta historia deben quedar en `CHANGELOG.md` como establece la constitución.
