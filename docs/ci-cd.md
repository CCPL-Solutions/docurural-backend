# Estrategia de Ramas y Flujo CI/CD

## 1. Ramas

| Rama | Propósito | Ambiente |
|---|---|---|
| `main` | Refleja producción. Solo recibe merges de releases ya certificadas. | Producción |
| `develop` | Integración continua. Recibe merges de `feature/*` vía PR. | Desarrollo |
| `release/x.y.z` | Punto de corte para certificación. Se crea desde `develop`. | QA |
| `hotfix/x.y.z` | Corrección urgente sobre producción. Sale de `main`. | QA → Producción |

Ramas de trabajo de corta duración:

- `feature/*` → sale de `develop`, vuelve a `develop` vía PR.
- `bugfix/*` → sale de `release/x.y.z` cuando QA reporta un bug, vuelve a esa misma `release/x.y.z` vía PR.
- `hotfix/*` → sale de `main`.

**Reglas:** nunca push directo a `main`, `develop` o `release/*` — todo entra vía PR con ≥1 aprobación y CI en verde. Mientras una `release/x.y.z` está en certificación, no se agregan features nuevas a esa rama, solo fixes.

## 2. Flujo de bug durante certificación

1. `bugfix/QA-123-descripcion` desde `release/x.y.z`.
2. PR hacia `release/x.y.z` (no hacia `develop` todavía). Corre CI.
3. Al mergear, `cd-qa.yml` redespliega automáticamente a QA con una nueva versión `rc.N+1`.
4. QA repite la certificación (regresión sobre lo que falló).
5. Al cerrar la release (merge a `main`), `release-backmerge.yml` abre automáticamente un PR de `release/x.y.z` → `develop` para no perder los fixes acumulados.

## 3. Versionado

La versión semántica (`1.4.0`) se mantiene fija durante todo el ciclo de certificación. Cada intento de certificación genera un build identificable:

| Momento | Versión |
|---|---|
| Push a `develop` | `1.4.0-SNAPSHOT` (la del `pom.xml`) |
| Primer deploy a QA (`release/1.4.0`) | `1.4.0-rc.1` |
| Fix del bug 1 | `1.4.0-rc.2` |
| Tag `v1.4.0` → Producción | `1.4.0` |

El `x.y.z` de cada `rc.N` se toma del **nombre de la rama** (`release/1.4.0`, `hotfix/1.4.1`); el número de build (`rc.N`) es `github.run_number`. Ver `.github/workflows/cd-qa.yml`.

QA confirma exactamente qué build está probando con:

```bash
curl https://<host-qa>/api/version
```

que devuelve `version`, `commit`, `branch` y `buildTime` (`co.edu.docurural.health.controller.VersionController`, poblado por el goal `build-info` de `spring-boot-maven-plugin` en `pom.xml`).

### Coordinación con el repo del front (Angular)

Front y back son repos independientes. Usar el mismo número base `x.y.z-rc.N` en ambos y documentar en el ticket de release qué combinación de versiones fue certificada junta.

## 4. Pipelines (`.github/workflows/`)

| Workflow | Disparador | Qué hace |
|---|---|---|
| `ci.yml` | Push a `feature/*`, `bugfix/*`, `hotfix/*`; PR hacia `develop`, `main`, `release/*`, `hotfix/*` | `mvn verify` (tests + gate JaCoCo 80%/65%) |
| `cd-dev.yml` | Push a `develop` | Despliega a Desarrollo con la versión `SNAPSHOT` del pom |
| `cd-qa.yml` | Push a `release/**` o `hotfix/**` | Calcula `x.y.z-rc.N` desde el nombre de rama y despliega a QA |
| `cd-prod.yml` | Push de un tag `v*.*.*` | Verifica que el tag esté en `main`, despliega a Producción (requiere aprobación del Environment `production`), publica GitHub Release |
| `release-backmerge.yml` | Se cierra (merge) un PR de `release/*`/`hotfix/*` hacia `main` | Abre PR automático de esa rama hacia `develop` |
| `_deploy.yml` | Reutilizable (`workflow_call`) | Lógica común de build + deploy + health check + rollback que usan los tres `cd-*.yml` |

Cada despliegue verifica `GET /api/actuator/health` (reintentos con backoff) y `GET /api/version` (que la versión reportada coincida con la esperada) antes de darse por exitoso. Si falla, revierte automáticamente al JAR anterior (`_deploy.yml`, paso "Rollback automático").

## 5. Protecciones de rama recomendadas (configurar en GitHub)

- `main`, `develop`, `release/*`: sin push directo, solo PR, ≥1 aprobación, status check `build-and-test` en verde.
- `main`: adicionalmente restringir quién puede pushear directamente y prohibir force-push.
- Environment `production`: required reviewers, y restringido a que solo tags `v*` puedan desplegar.
