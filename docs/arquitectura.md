[← Volver al README](../README.md)

# Arquitectura del proyecto

El proyecto sigue una organización **package-by-feature** (vertical slice) para mejorar la cohesión y la
navegabilidad: cada módulo agrupa sus propias capas (`controller`, `dto`, `entity`, `enums`, `mapper`, `repository`,
`service`) en lugar de dispersarlas en paquetes técnicos transversales.

```
src/main/java/co/edu/docurural/
├── auth/          # Autenticación
├── user/          # Gestión de usuarios
├── document/       # Gestión y búsqueda de documentos (incluye storage/ para S3 y disco local)
├── category/        # Categorías documentales
├── activitylog/       # Auditoría de acciones (sin controller ni DTO propios)
├── dashboard/           # Panel de control — endpoint agregado
├── health/                # Metadata de build
└── shared/                   # Código compartido entre módulos: config, security, exception, audit, enums, dto, util
```

Dentro de cada módulo, las capas típicas son:

| Subpaquete    | Contenido                                              |
|---------------|-----------------------------------------------------------|
| `controller`  | Endpoints REST (`@RestController`)                             |
| `dto`         | Records de entrada/salida (request/response)                       |
| `entity`      | Entidades JPA (solo en módulos con persistencia propia)                |
| `enums`       | Enumeraciones de dominio del módulo                                        |
| `mapper`      | Mappers MapStruct entidad ↔ DTO                                                |
| `repository`  | Repositorios Spring Data (+ proyecciones y specifications si aplica)               |
| `service`     | Interfaz + implementación (`{Dominio}Service` / `{Dominio}ServiceImpl`)                |

No todos los módulos tienen todas las capas: `activitylog` no expone `controller` ni `dto` (es auditoría interna),
`dashboard` y `health` no tienen `entity`/`repository` propios (agregan datos de otros módulos), y `auth` no tiene
`entity` (reutiliza `User` del módulo `user`).
