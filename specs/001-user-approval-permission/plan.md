# Implementation Plan: Permiso para aprobar documentos (HU-32)

**Branch**: `feature/hu-32` (directorio de spec: `001-user-approval-permission`) | **Date**: 2026-09-20 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-user-approval-permission/spec.md`

## Summary

Añadir a `users` un indicador booleano `can_approve` (por defecto `false`), independiente del rol
pero nunca activo en `READER`. Se extiende el CRUD de usuarios existente (crear, editar, consultar,
listar) para aceptarlo y exponerlo, se aplican las reglas de integridad en `UserServiceImpl`
(rechazo al crear/editar un READER con el permiso, retiro automático al cambiar el rol a READER),
se audita el cambio en la bitácora (`EDIT_USER`, `can_approve: a → b`) y se entrega
`UserService.isActiveApprover(userId)`, una consulta directa a base de datos (sin caché ni token)
que las historias posteriores de aprobación usarán para que retirar el permiso surta efecto de
inmediato. No hay endpoints nuevos: solo campos nuevos en contratos existentes.

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**: Spring Boot 3.5.x, Spring Data JPA, Spring Security (`@PreAuthorize`), MapStruct (mappers abstractos), Lombok, Bean Validation, Flyway

**Storage**: PostgreSQL; esquema por Flyway, nueva migración `V3__add_can_approve_to_users.sql`

**Testing**: JUnit 5 + Mockito (`MockitoExtension`, `@Spy` de mappers reales), `@WebMvcTest` para el controlador; `TestFixtures`

**Target Platform**: Servicio web REST (JVM, AWS)

**Project Type**: web-service (backend; la UI es del cliente web, fuera de alcance)

**Performance Goals**: `isActiveApprover` es una consulta `exists` por PK; sin requisito adicional

**Constraints**: cobertura JaCoCo ≥80% líneas / ≥65% ramas; migraciones inmutables; el permiso NO viaja en el JWT (FR-011)

**Scale/Scope**: decenas de usuarios; 1 columna, ~5 clases modificadas, 0 clases nuevas de producción

Sin `NEEDS CLARIFICATION` pendientes (ver [research.md](research.md)).

## Constitution Check

*GATE: pasa antes de Phase 0; re-evaluado tras Phase 1.*

| Principio | Estado | Evidencia |
|-----------|--------|-----------|
| I. Módulos por feature | ✅ | Todo vive en `user`. `activitylog` se usa vía `ActivityLogService`. `isActiveApprover` es la interfaz que otros módulos usarán; ninguno tocará `UserRepository`. |
| II. DI e inmutabilidad | ✅ | Sin colaboradores nuevos; DTO como `record`. |
| III. Simplicidad / fail fast | ✅ | Validaciones al inicio; métodos privados ≤3 parámetros; sin flags booleanos que bifurquen (se separan `resolveCanApproveOnCreate` / `resolveCanApproveOnUpdate`). Ver nota en Complexity Tracking sobre el tamaño de `UserServiceImpl`. |
| IV. Contrato de pruebas | ✅ | Pruebas nuevas en `UserServiceTest`, `UserControllerWebMvcTest`, `UserMapperTest`, `UpdateUserRequestDtoTest`; fixtures en `TestFixtures`. |
| V. Cobertura | ✅ | Todas las ramas nuevas (READER×crear, READER×editar, cambio de rol, omitido, sin cambio) tienen prueba. Sin nuevas exclusiones. |
| VI. Migraciones inmutables | ✅ | Migración V3 nueva, idempotente; V1 intacta. |
| VII. Borrado lógico | ✅ | Desactivar conserva `can_approve` (FR-007); no hay `DELETE`. |
| VIII. Errores / i18n / auditoría | ✅ | `BusinessRuleException(INVALID_ARGUMENT)` con clave `user.can-approve.reader-not-allowed`; no se modifica `GlobalExceptionHandler`; `AuditContext` ya presente; `ActivityLogService` sin cambios. |
| IX. Seguridad | ⚠️ justificado | Endpoints ya son ADMIN-only (`UserController`, se verifica con prueba). Nuevo componente `canApprove` en `RequestDto`: ver Complexity Tracking. |

**Post-diseño**: sin cambios respecto a la tabla; la única desviación está justificada abajo.

## Project Structure

### Documentation (this feature)

```text
specs/001-user-approval-permission/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── users-api.md
└── tasks.md             # /speckit-tasks (no lo crea este comando)
```

### Source Code (repository root)

```text
src/main/
├── java/co/edu/docurural/user/
│   ├── entity/User.java                    # + boolean canApprove
│   ├── dto/CreateUserRequestDto.java       # + Boolean canApprove (nullable)
│   ├── dto/UpdateUserRequestDto.java       # + Boolean canApprove (nullable)
│   ├── dto/UserResponseDto.java            # + boolean canApprove
│   ├── dto/CreateUserResponseDto.java      # + boolean canApprove
│   ├── dto/UpdateUserResponseDto.java      # + boolean canApprove
│   ├── mapper/UserMapper.java              # sin cambios de código (mapeo por nombre); verificado en prueba
│   ├── repository/UserRepository.java      # + existsByIdAndCanApproveTrueAndStatusAndRoleNot
│   └── service/
│       ├── UserService.java                # + isActiveApprover(Long)
│       └── UserServiceImpl.java            # reglas de create/update, auditoría
└── resources/
    ├── db/migration/V3__add_can_approve_to_users.sql
    └── messages.properties                 # + user.can-approve.reader-not-allowed

src/test/java/co/edu/docurural/
├── support/TestFixtures.java               # + userApprover(id); builders con canApprove
└── user/{service,controller,mapper,dto}/   # pruebas ampliadas

CHANGELOG.md                                # + entrada Added bajo [Unreleased]
```

**Structure Decision**: proyecto único Spring Boot ya existente; la funcionalidad extiende el
paquete `user` sin crear paquetes ni clases de producción nuevas.

## Complexity Tracking

| Desviación | Por qué | Alternativa más simple descartada |
|------------|---------|-----------------------------------|
| `canApprove` en `CreateUserRequestDto`/`UpdateUserRequestDto` como `@Nullable Boolean` sin anotación de `jakarta.validation.constraints` (Principio IX exige una) | FR-002 y FR-012 requieren tres estados (`true`, `false`, omitido → valor por defecto / conservar), y ninguna restricción de Bean Validation acota un `Boolean` nulable. `@NotNull` contradiría ambos requisitos. El rango de valores ya está acotado por el tipo y la regla de negocio del rol se valida en el servicio. **Registrar en la descripción del PR.** | `@NotNull`: rompe FR-002/FR-012. Restricción personalizada: añade una clase por una regla que el tipo ya garantiza. Si el revisor lo exige, se reabre esta decisión. |
| `UserServiceImpl` crece (ya inyecta 6 colaboradores) | La regla es un caso de uso de usuario; extraer una clase sin estado añadiría un séptimo colaborador sin reducir la complejidad. Se mantiene en métodos privados pequeños. | Clase `ApprovalPermissionPolicy`: prematura para dos reglas. |
