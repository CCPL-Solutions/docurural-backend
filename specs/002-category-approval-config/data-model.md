# Data Model: Configuración de aprobación por categoría (HU-31)

## Entidad `Category` (tabla `categories`) — cambios

| Campo Java | Columna | Tipo | Restricciones | Notas |
|------------|---------|------|---------------|-------|
| `requiresApproval` | `requires_approval` | `boolean` / `BOOLEAN` | `NOT NULL DEFAULT FALSE` | Nuevo. `@Builder.Default = false`. |

Migración: `V4__add_requires_approval_to_categories.sql`. Las 8 categorías sembradas en V2 quedan
en `false` por el `DEFAULT`. No se añade índice: la consulta es por PK.

## Reglas

| # | Regla | Dónde se aplica | Fuente |
|---|-------|-----------------|--------|
| V1 | Crear con `requiresApproval` nulo u omitido → `false` | `CategoryServiceImpl.create` | FR-002 |
| V2 | Editar con `requiresApproval` nulo u omitido → conserva el valor; cuenta como "sin cambio" | `CategoryServiceImpl.update` | FR-003 |
| V3 | Solo ADMIN crea o edita | `@PreAuthorize` existente en `CategoryController` | FR-004 |
| V4 | Categoría inactiva no editable (regla existente) | `Category.assertEditable` | Edge cases |
| V5 | Cambiar el indicador no toca documentos | `CategoryServiceImpl.update` (sin llamada a `document`) | FR-008 |

## Transiciones de `requiresApproval`

| Evento | Antes → después | Aviso de alcance | Advertencia (si aprobadores activos < 2) | Bitácora |
|--------|-----------------|------------------|-------------------------------------------|----------|
| Crear sin campo / `false` | — → `false` | no | no | `CREATE_CATEGORY`: `Categoria creada: X (requires_approval: false)` |
| Crear con `true` | — → `true` | no | sí | `CREATE_CATEGORY`: `Categoria creada: X (requires_approval: true)` |
| Editar `false → true` | `false` → `true` | sí | sí | `EDIT_CATEGORY` incluye `requires_approval: false → true` |
| Editar `true → false` | `true` → `false` | sí | no | `EDIT_CATEGORY` incluye `requires_approval: true → false` |
| Editar con el mismo valor u omitido | sin cambio | no | no | sin la línea `requires_approval` |

## Valor derivado (no almacenado)

```text
activeApprovers = count(users WHERE can_approve AND status = 'ACTIVE' AND role <> 'READER')
advertencia     = indicador pasa a true AND activeApprovers < 2
```

Se calcula en `UserService.countActiveApprovers()` solo cuando el indicador pasa a `true`.

## Objetos de transporte

- `ApprovalNotices(String scopeNotice, String approverWarning)`: `record` interno del paquete
  `category.dto` que agrupa los dos textos opcionales que el servicio pasa al mapper de edición.
- Campos nuevos en DTO: ver [contracts/categories-api.md](contracts/categories-api.md).

## Bitácora (`activity_log`)

Sin cambios de esquema ni de `ActivityAction`. Se reutilizan `CREATE_CATEGORY` y `EDIT_CATEGORY`;
solo cambia el texto de `detail`.

## Fuera de alcance

Estados de flujo del documento (Borrador, En revisión), su transición al cargar y el aviso en la
carga de documentos: HU-33 y posteriores, que consumirán `CategoryQueryService.requiresApproval`.
