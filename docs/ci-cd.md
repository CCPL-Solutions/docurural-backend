[← Volver al README](../README.md)

# CI/CD y flujo de ramas

El proyecto sigue **GitFlow**. Nunca se hace push directo a `main`, `develop` o `release/*`: todo cambio entra por
pull request, con al menos una aprobación y CI en verde.

| Rama              | Ambiente                | Origen                                    |
|--------------------|----------------------------|------------------------------------------------|
| `main`               | Producción                    | tag `v*.*.*`                                       |
| `develop`              | Desarrollo                      | `feature/*` mergeado vía PR                            |
| `release/x.y.z`          | QA / certificación                | rama de estabilización desde `develop`                    |
| `hotfix/x.y.z`              | QA (y luego producción)              | corrección urgente desde `main`                                |

## Workflows (`.github/workflows/`)

| Workflow                    | Disparador                                                                                            | Qué hace                                                                                                        |
|-------------------------------|-----------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `ci.yml`                        | Push a `feature/**`, `bugfix/**`, `hotfix/**`         | `mvn verify` (incluye el gate de cobertura JaCoCo); publica el reporte de pruebas como artefacto. No se re-ejecuta al crear/actualizar el PR: el check que ve GitHub es el de la ejecución sobre el último push a la rama. Corre siempre con el perfil `test` (fijado por `maven-surefire-plugin`), por lo que nunca necesita credenciales AWS.                        |
| `cd-dev.yml`                       | Push a `develop` (+ manual)                                                                                     | Construye y despliega a Desarrollo (perfil `develop`).                                                                          |
| `cd-qa.yml`                           | Push a `release/**`/`hotfix/**` (+ manual)                                                                          | Despliega a QA con versión `x.y.z-rc.<run_number>`.                                                                            |
| `cd-prod.yml`                            | Tag `v*.*.*` (+ manual)                                                                                                | Despliega a Producción; requiere aprobación manual antes y verificación manual después del despliegue.                           |
| `_deploy.yml`                               | Reutilizable (`workflow_call`)                                                                                            | Lógica común de despliegue: build, backup del JAR anterior, reemplazo, health check y rollback automático si falla.                 |
| `release-backmerge.yml`                        | PR cerrado y mergeado hacia `main`                                                                                            | Abre automáticamente un PR de vuelta a `develop` desde la rama `release/*`/`hotfix/*` correspondiente.                                |

## Despliegue

El backend **no se contenedoriza** (no hay `Dockerfile` ni `docker-compose` en el repositorio). El pipeline de CD
compila un JAR, lo copia a `/opt/docurural/backend/docurural-api.jar` en un runner self-hosted y lo administra con
**systemd** (`systemctl start docurural`). Tras el reemplazo, se verifica `GET /api/actuator/health` y que
`GET /api/version` reporte la versión esperada; si el arranque o el health check fallan, el pipeline revierte
automáticamente al JAR anterior.

Los perfiles `develop`, `qa` y `prod` importan sus secretos desde AWS Parameter Store sin el prefijo
`optional:` (fail-fast): si Parameter Store no responde al arrancar, el health check falla y el
rollback automático restaura el JAR anterior — que tiene la misma configuración y fallaría igual, así
que el rollback no repara una caída de SSM, solo evita servir con secretos vacíos.
