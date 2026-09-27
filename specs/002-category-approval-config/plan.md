# Implementation Plan: Configuración de aprobación por categoría (HU-31)

**Branch**: `feature/hu-31` (directorio de spec: `002-category-approval-config`) | **Date**: 2026-09-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-category-approval-config/spec.md`

## Summary

Añadir a `categories` un indicador booleano `requires_approval` (por defecto `false`, también en
las 8 categorías sembradas). Se extiende el CRUD de categorías existente (crear, editar, detalle,
listado) para aceptarlo y exponerlo. Al guardar, la respuesta incluye un aviso de alcance cuando
el valor cambia y una advertencia no bloqueante cuando pasa a `true` con menos de dos aprobadores
activos (conteo nuevo `UserService.countActiveApprovers()`). La bitácora registra
`requires_approval: a → b` en `EDIT_CATEGORY` y el valor inicial en `CREATE_CATEGORY`. Para la carga
de documentos (HU-33) se entrega `CategoryQueryService.requiresApproval(categoryId)`, una interfaz
de solo lectura sin dependencia de `document` que evita el ciclo descrito en la desviación D-4. No
hay endpoints nuevos ni cambios de autorización.

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**: Spring Boot 3.5.x, Spring Data JPA, Spring Security (`@PreAuthorize`), MapStruct (mappers abstractos), Lombok, Bean Validation, Flyway

**Storage**: PostgreSQL; nueva migración `V4__add_requires_approval_to_categories.sql`

**Testing**: JUnit 5 + Mockito (`MockitoExtension`, `@Spy` de mappers reales), `@WebMvcTest` para el controlador; `TestFixtures`

**Target Platform**: Servicio web REST (JVM, AWS)

**Project Type**: web-service (backend; la UI es del cliente web, fuera de alcance)

**Performance Goals**: `requiresApproval` es una lectura por PK; `countActiveApprovers` es un `count` sobre decenas de usuarios y solo se ejecuta al activar el indicador

**Constraints**: cobertura JaCoCo ≥80% líneas / ≥65% ramas; migraciones inmutables; cambiar el indicador no toca documentos (FR-008)

**Scale/Scope**: 8–20 categorías, decenas de usuarios; 1 columna, 2 clases de producción nuevas (`CategoryQueryService`/`Impl`) y 1 `record` interno (`ApprovalNotices`), ~10 clases modificadas

Sin `NEEDS CLARIFICATION` pendientes (ver [research.md](research.md)).

## Constitution Check

*GATE: pasa antes de Phase 0; re-evaluado tras Phase 1.*

| Principio | Estado | Evidencia |
|-----------|--------|-----------|
| I. Módulos por feature | ✅ | El conteo de aprobadores se pide a `UserService` (no a `UserRepository`). `CategoryQueryService` es la interfaz que `document` usará en HU-33, sin ciclo (R3). La consulta de `users` sigue en `user`. |
| II. DI e inmutabilidad | ✅ | Constructor con `@RequiredArgsConstructor`; colaboradores declarados como interfaz (`UserService`); DTO y `ApprovalNotices` como `record`. |
| III. Simplicidad / fail fast | ✅ | Mappers en ≤3 parámetros gracias a `ApprovalNotices` (R5); sin flags booleanos que bifurquen (el aviso y la advertencia se calculan en métodos privados separados); `requiresApproval` no devuelve `null` (lanza 404). |
| IV. Contrato de pruebas | ✅ | `CategoryServiceTest`, `CategoryQueryServiceTest` (nueva), `CategoryMapperTest`, `CategoryControllerWebMvcTest`, `UserServiceTest`; fixtures nuevos en `TestFixtures`. |
| V. Cobertura | ✅ | Cada rama nueva (omitido, sin cambio, `false→true`, `true→false`, 0/1/2 aprobadores, no existe) tiene prueba. Sin nuevas exclusiones. |
| VI. Migraciones inmutables | ✅ | V4 nueva e idempotente (`ADD COLUMN IF NOT EXISTS`); V1–V3 intactas. |
| VII. Borrado lógico | ✅ | Sin `DELETE`; activar/desactivar la categoría no toca el indicador. |
| VIII. Errores / i18n / auditoría | ✅ | Textos por `MessageResolver` (2 claves nuevas); `ResourceNotFoundException` existente; `ActivityLogService` sin cambios; `AuditContext` ya presente en `create`/`update`. |
| IX. Seguridad | ⚠️ justificado | Reglas de autorización existentes sin cambios (ADMIN para escribir, ADMIN/EDITOR para leer). Componente `requiresApproval` `@Nullable Boolean` sin restricción: ver Complexity Tracking. |

**Post-diseño**: sin cambios respecto a la tabla; las desviaciones están justificadas abajo.

## Project Structure

### Documentation (this feature)

```text
specs/002-category-approval-config/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── categories-api.md
└── tasks.md             # /speckit-tasks (no lo crea este comando)
```

### Source Code (repository root)

```text
src/main/
├── java/co/edu/docurural/category/
│   ├── entity/Category.java                       # + boolean requiresApproval (@Builder.Default false)
│   ├── dto/CreateCategoryRequestDto.java          # + @Nullable Boolean requiresApproval
│   ├── dto/UpdateCategoryRequestDto.java          # + @Nullable Boolean requiresApproval
│   ├── dto/CreateCategoryResponseDto.java         # + boolean requiresApproval, String approverWarning
│   ├── dto/UpdateCategoryResponseDto.java         # + boolean requiresApproval, String approvalScopeNotice, String approverWarning
│   ├── dto/CategoryDetailResponseDto.java         # + boolean requiresApproval (detalle y listado)
│   ├── dto/ApprovalNotices.java                   # NUEVO record interno (scopeNotice, approverWarning)
│   ├── mapper/CategoryMapper.java                 # firmas de toCreateResponse / toUpdateResponse
│   └── service/
│       ├── CategoryServiceImpl.java               # reglas de create/update, aviso, advertencia, auditoría
│       ├── CategoryQueryService.java              # NUEVO: requiresApproval(Long)
│       └── CategoryQueryServiceImpl.java          # NUEVO: solo CategoryRepository + MessageResolver
├── java/co/edu/docurural/user/
│   ├── repository/UserRepository.java             # + countByCanApproveTrueAndStatusAndRoleNot
│   └── service/UserService{,Impl}.java            # + countActiveApprovers()
└── resources/
    ├── db/migration/V4__add_requires_approval_to_categories.sql
    └── messages.properties                        # + category.requires-approval.{scope-notice,few-approvers}

src/test/java/co/edu/docurural/
├── support/TestFixtures.java                      # + categoryRequiringApproval(id, name); request builders con requiresApproval
├── category/{service,mapper,controller}/          # pruebas ampliadas + CategoryQueryServiceTest
└── user/service/UserServiceTest.java              # + countActiveApprovers

CHANGELOG.md                                       # + entrada Added bajo [Unreleased]
```

**Structure Decision**: proyecto único Spring Boot ya existente; la funcionalidad extiende el
paquete `category` (y añade un método a `user`) sin crear paquetes nuevos.

## Complexity Tracking

| Desviación | Por qué | Alternativa más simple descartada |
|------------|---------|-----------------------------------|
| `requiresApproval` en `CreateCategoryRequestDto`/`UpdateCategoryRequestDto` como `@Nullable Boolean` sin anotación de `jakarta.validation.constraints` (Principio IX exige una) | FR-002 y FR-003 requieren tres estados (`true`, `false`, omitido → por defecto / conservar), y ninguna restricción de Bean Validation acota un `Boolean` nulable. Es la misma decisión aceptada para `canApprove` en HU-32. **Registrar en la descripción del PR.** | `@NotNull`: rompe FR-002/FR-003 y al cliente web actual. Restricción personalizada: una clase por una regla que el tipo ya garantiza. |
| `CategoryServiceImpl` pasa de 8 a 9 colaboradores (`+ UserService`) | La advertencia es parte del caso de uso de guardar una categoría y necesita el conteo de `user`. Extraer una clase "asesora" añadiría igualmente un colaborador sin reducir la complejidad. | Componente `ApprovalConfigAdvisor`: prematuro para una comparación con un umbral. |
| Interfaz nueva `CategoryQueryService` en lugar del método en `CategoryService` que pedía la clarificación | Evita el ciclo `category ↔ document` que D-4 anticipa para HU-33, siguiendo la solución que la propia D-4 prescribe. La spec (FR-014) se actualizó en consecuencia. | Método en `CategoryService`: HU-33 no podría inyectarlo sin ciclo o sin `@Lazy`. |
