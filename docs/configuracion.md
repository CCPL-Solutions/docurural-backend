[← Volver al README](../README.md)

# Configuración del entorno

Copia el archivo de ejemplo y completa las variables:

```bash
cp .env.example .env
```

| Variable                             | Descripción                                             | Valor por defecto       |
|--------------------------------------|---------------------------------------------------------|-------------------------|
| `DB_HOST`                            | Host de PostgreSQL                                      | `localhost`             |
| `DB_PORT`                            | Puerto de PostgreSQL                                    | `5432`                  |
| `DB_NAME`                            | Nombre de la base de datos                              | `docurural_db`          |
| `DB_USER`                            | Usuario de la base de datos                             | `docurural`             |
| `DB_PASSWORD`                        | Contraseña del usuario de BD                            | —                       |
| `JWT_SECRET`                         | Clave secreta para firmar tokens JWT (mínimo 32 bytes)  | —                       |
| `JWT_EXPIRATION_MS`                  | Tiempo de vida del token en milisegundos                | `1800000` (30 min)      |
| `JWT_ISSUER`                         | Emisor incluido en el claim `iss` del JWT               | `docurural`             |
| `SPRING_PROFILES_ACTIVE`             | Perfil activo (`local`, `develop`, `qa` o `prod`)       | `local`                 |
| `CORS_ALLOWED_ORIGINS`               | Orígenes permitidos en CORS                             | `http://localhost:4200` |
| `ADMIN_SEED_EMAIL`                   | Email del administrador inicial (opcional, idempotente) | —                       |
| `ADMIN_SEED_PASSWORD`                | Contraseña del administrador inicial (opcional)         | —                       |
| `DOCURURAL_STORAGE_PROVIDER`         | Proveedor de almacenamiento (`local` o `s3`)            | `s3`                    |
| `DOCURURAL_STORAGE_BASE_PATH`        | Directorio base para archivos locales                   | `./uploads/documents`   |
| `DOCURURAL_STORAGE_MAX_FILE_SIZE`    | Tamaño máximo por archivo                               | `10MB`                  |
| `DOCURURAL_STORAGE_MAX_REQUEST_SIZE` | Tamaño máximo por request (carga por lote)              | `55MB`                  |
| `DOCURURAL_STORAGE_S3_BUCKET`        | Nombre del bucket S3                                    | —                       |
| `AWS_REGION`                         | Región AWS                                              | `us-east-1`             |
| `DOCURURAL_STORAGE_S3_KEY_PREFIX`    | Prefijo de llaves en S3                                 | `documents`             |
| `DOCURURAL_BCRYPT_STRENGTH`          | Factor de coste de BCrypt                               | `12`                    |

## Perfiles de Spring

Los perfiles se configuran en `application-<perfil>.yaml` y se activan con `SPRING_PROFILES_ACTIVE`.
Además existe `application-test.yaml`, usado únicamente por la suite de pruebas (ver más abajo).

| Perfil    | Archivo                    | Descripción                                                                                                                                                                    |
|-----------|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `local`   | `application-local.yaml`   | Portátil del desarrollador. Almacenamiento en S3 (bucket de `develop`) vía credenciales AWS propias del desarrollador; Parameter Store deshabilitado. Logs SQL + `DEBUG`.      |
| `develop` | `application-develop.yaml` | Entorno desplegado en AWS (EC2 + PostgreSQL local a la instancia, `dev.ccplsolutions.link`). Importa secretos de `/docurural/develop/` en Parameter Store. Logs SQL + `DEBUG`. |
| `qa`      | `application-qa.yaml`      | Entorno de certificación. Importa secretos de `/docurural/qa/` en Parameter Store.                                                                                             |
| `prod`    | `application-prod.yaml`    | Producción. Importa secretos de `/docurural/prod/`. Swagger UI y `/v3/api-docs` deshabilitados.                                                                                |

La resolución de secretos sigue un orden de prioridad: **Parameter Store → variable de entorno → valor por defecto**.
En `local`, donde no hay Parameter Store, todo sale de las variables de entorno. En `develop`, `qa` y
`prod` el import **no** lleva el prefijo `optional:` — si Parameter Store no responde al arrancar, la
aplicación falla en el arranque en vez de continuar con secretos vacíos (fail-fast).

`mvn verify`/CI siempre corren con el perfil `test` (fijado por `maven-surefire-plugin` en el
`pom.xml`, independientemente de lo que tengas exportado en tu shell), así que nunca requieren
credenciales AWS.

## AWS Parameter Store

Los perfiles `develop`, `qa` y `prod` importan secretos automáticamente desde Parameter Store al
arrancar, bajo el prefijo `/docurural/<entorno>/`:

| Clave SSM           | Propiedad Spring                                                |
|---------------------|-----------------------------------------------------------------|
| `db-password`       | `spring.datasource.password`                                    |
| `jwt-secret`        | `docurural.security.jwt.secret`                                 |
| `s3-bucket-docs`    | `docurural.storage.s3.bucket`                                   |
| `s3-bucket-backups` | (sin consumidor en la app; reservado para el script de backups) |
| `aws-region`        | `docurural.storage.s3.region`                                   |

La región del cliente de Parameter Store se fija de forma estática en `application.yaml`
(`spring.cloud.aws.region.static`) para no depender del IMDS de EC2 ni de un `~/.aws/config`
ambiental — necesario porque esa resolución ocurre antes de que se procesen los perfiles.

## Credenciales AWS para el perfil `local`

El perfil `local` usa S3 (el bucket de `develop`, bajo el prefijo `documents/local`), así que necesita
credenciales AWS propias del desarrollador:

```bash
aws configure sso --profile docurural-dev
aws sso login --profile docurural-dev

export AWS_PROFILE=docurural-dev
export SPRING_PROFILES_ACTIVE=local
./mvnw spring-boot:run
```
