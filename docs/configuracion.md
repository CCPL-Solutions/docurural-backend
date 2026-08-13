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
| `SPRING_PROFILES_ACTIVE`             | Perfil activo (`dev`, `qa` o `prod`)                    | `dev`                   |
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

| Perfil | Archivo                    | Descripción                                                                                     |
|--------|----------------------------|-------------------------------------------------------------------------------------------------|
| `dev`  | `application-develop.yaml` | Logs SQL formateados + nivel `DEBUG`. Sin Parameter Store; secretos vienen del `.env`.          |
| `qa`   | `application-qa.yaml`      | Entorno de certificación. Importa secretos de `/docurural/qa/` en Parameter Store.              |
| `prod` | `application-prod.yaml`    | Producción. Importa secretos de `/docurural/prod/`. Swagger UI y `/v3/api-docs` deshabilitados. |

La resolución de secretos sigue un orden de prioridad: **Parameter Store → variable de entorno → valor por defecto**.
En `develop`, donde no hay Parameter Store configurado, basta con definir las variables en el `.env`.

## AWS Parameter Store

Los perfiles `qa` y `prod` importan secretos automáticamente desde Parameter Store al arrancar:

| Clave SSM        | Propiedad Spring                |
|------------------|---------------------------------|
| `db-password`    | `spring.datasource.password`    |
| `jwt-secret`     | `docurural.security.jwt.secret` |
| `s3-bucket-docs` | `docurural.storage.s3.bucket`   |
| `aws-region`     | `docurural.storage.s3.region`   |

## Credenciales AWS en desarrollo local

```bash
aws configure sso --profile docurural-dev
aws sso login --profile docurural-dev

export AWS_PROFILE=docurural-dev
export AWS_REGION=us-east-1
./mvnw spring-boot:run
```
