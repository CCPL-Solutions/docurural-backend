# Constitución de DocuRural Backend

Sistema de gestión documental y archivo digital de la IERD Mina y Ticha. Esta constitución define
las reglas no negociables del proyecto. Prevalece sobre cualquier otra práctica, preferencia
personal o convención heredada.

## Principios Fundamentales

### I. Arquitectura Modular por Feature (NO NEGOCIABLE)

El código MUST organizarse en paquetes por feature: `auth`, `user`, `document`, `category`,
`activitylog`, `dashboard`, `health` y `shared`.

Un módulo MUST acceder a otro únicamente a través de sus interfaces de servicio. Un módulo
MUST NOT inyectar el repositorio de otro módulo ni consultar sus tablas directamente, ni por
JPQL ni por SQL nativo.

Todo servicio nuevo MUST tener interfaz (`{Dominio}Service`) e implementación separada
(`{Dominio}ServiceImpl`, anotada con `@Service`).

**Rationale:** la frontera entre módulos es lo único que impide que siete features acopladas se
conviertan en un monolito imposible de cambiar por partes. Es verificable de forma mecánica:
revisar los imports cruzados y las tablas referenciadas en cada repositorio.

### II. Inversión de Dependencias e Inmutabilidad

Las dependencias MUST inyectarse por constructor mediante `@RequiredArgsConstructor` de Lombok.
`@Autowired` en campos o en setters está prohibido.

Un servicio MUST declarar sus colaboradores como la interfaz, nunca como la clase `Impl`.

Los campos de servicios y componentes MUST ser `final`. Todos los DTO de request y response
MUST ser `record` de Java.

**Rationale:** la inyección por constructor hace explícitas y obligatorias las dependencias de
cada clase, y es lo que permite instanciar un servicio en un test unitario sin levantar el
contexto de Spring (Principio IV). La inmutabilidad por defecto elimina una categoría entera de
errores de estado compartido.

### III. Simplicidad y Fail Fast

Cada clase MUST tener una sola razón para cambiar: un servicio orquesta un caso de uso, un mapper
solo mapea, un validador solo valida.

Un método MUST aceptar como máximo 3 parámetros; si necesita más, MUST agruparse en un `record`.
Un método MUST NOT recibir un flag booleano que bifurque su comportamiento: en ese caso se
divide en dos métodos.

Las precondiciones MUST validarse al inicio del método, lanzando la excepción inmediatamente.

Un método público MUST NOT retornar `null`; para un resultado que puede no existir se usa
`Optional<T>`.

MUST NOT quedar código comentado en el repositorio, ni bloques `catch` que ignoren la excepción:
lo capturado se maneja o se relanza con contexto añadido.

Los comentarios SHOULD explicar el porqué de una decisión no obvia, una restricción externa o un
workaround. No el qué, que es trabajo de los nombres.

**Rationale:** este es el subconjunto de clean code que se puede verificar en una revisión de PR
sin entrar en discusiones de gusto. Todo lo demás es convención y vive en `CLAUDE.md`.

### IV. Contrato de Pruebas por Capa (NO NEGOCIABLE)

Cada controlador MUST tener una clase `{Controlador}WebMvcTest` configurada con `@WebMvcTest`
excluyendo `SecurityConfig` y `JwtAuthenticationFilter` vía `excludeFilters`,
`@AutoConfigureMockMvc(addFilters = false)` e `@Import(GlobalExceptionHandler.class)`. Los
servicios se sustituyen con `@MockitoBean`. MUST NOT usarse `@SpringBootTest` para probar un
controlador.

Los servicios MUST probarse con `@ExtendWith(MockitoExtension.class)`, como pruebas unitarias
puras, sin levantar contexto de Spring. Los mappers colaboradores MUST inyectarse como `@Spy` con
la instancia real, para ejercitar la lógica de mapeo en lugar de simularla.

Los objetos de dominio de un test MUST construirse con los builders de
`src/test/java/co/edu/docurural/support/TestFixtures.java`, nunca con `new` directo.

Los nombres de los métodos de prueba MUST seguir el formato `<accion>_<contexto>_<resultado>`.

**Rationale:** separar la capa web de la capa de servicio mantiene la suite completa en segundos y
evita que un test de contrato HTTP falle por una razón de persistencia. Los fixtures centralizados
evitan que un cambio en una entidad obligue a tocar cuarenta tests.

### V. Cobertura Verificada como Puerta de Calidad

`./mvnw clean verify` MUST superar los umbrales de JaCoCo a nivel `BUNDLE`: **≥80% de líneas
(LINE)** y **≥65% de ramas (BRANCH)**, con las exclusiones ya declaradas en `pom.xml` (`**/dto/**`,
`**/entity/**`, `**/enums/**`, `**/repository/projection/**` y la clase principal de la
aplicación).

Bajar un umbral o añadir una exclusión nueva MUST justificarse explícitamente en la descripción
del PR y ser aprobado en la revisión.

El CI MUST bloquear el merge cuando `verify` falla.

**Rationale:** los umbrales ya están configurados y activos; el principio existe para que no se
relajen en silencio con el único fin de hacer pasar un PR con prisa.

### VI. Esquema Versionado con Migraciones Inmutables (NO NEGOCIABLE)

Todo cambio de esquema MUST expresarse como una migración nueva
`V{n+1}__{descripcion_con_guiones_bajos}.sql` en `src/main/resources/db/migration/`.

Una migración ya aplicada MUST NOT modificarse nunca, ni para corregir un error: Flyway detecta el
cambio de checksum y falla el arranque de la aplicación. Los errores se corrigen con una migración
correctiva posterior.

Las migraciones SHOULD ser idempotentes usando `IF NOT EXISTS` / `IF EXISTS`.

Las consultas de repositorio SHOULD usar métodos derivados de Spring Data. `@Query` con JPQL se
usa solo cuando la consulta derivada resultaría ilegible. `nativeQuery = true` se usa solo cuando
JPQL no es suficiente, y MUST documentar el motivo en un comentario sobre el método.

**Rationale:** editar una migración ya desplegada rompe el arranque en todos los entornos que la
tenían aplicada, incluido producción. El coste de una migración correctiva es de minutos; el de un
checksum roto en producción, de horas.

### VII. Borrado Lógico Universal

Ninguna lógica de negocio MUST ejecutar un `DELETE` físico sobre `users`, `categories` o
`documents`.

Las bajas MUST modelarse con la columna `status`: `ACTIVE` / `INACTIVE` en `users` y `categories`,
`ACTIVE` / `DELETED` en `documents`.

**Rationale:** `activity_log` referencia usuarios y documentos para construir la trazabilidad, que
es la razón de existir del sistema. Un borrado físico rompe esas referencias y con ellas el
historial de quién hizo qué.

### VIII. Errores Tipificados, Mensajes Externalizados y Auditoría No Intrusiva

Los errores de dominio MUST lanzarse como `BusinessRuleException` con un valor de
`BusinessErrorCode`. Añadir un error nuevo MUST hacerse extendiendo ese enum con su `HttpStatus`
asociado; MUST NOT modificarse `GlobalExceptionHandler`. Las excepciones de infraestructura
disponibles son `ResourceNotFoundException` (404), `ConflictException` (409) y
`FileStorageException` (500).

Los textos dirigidos al usuario MUST resolverse con `MessageResolver` sobre claves de
`src/main/resources/messages.properties`. MUST NOT haber strings de mensaje hardcodeados en código
de negocio.

Todo método de servicio que mute estado MUST recibir `AuditContext` (actorUserId + clientIp) como
parámetro explícito, generalmente el último.

`ActivityLogService` MUST conservar `@Transactional(propagation = Propagation.REQUIRES_NEW)`. Un
fallo de auditoría MUST NOT revertir la operación de negocio: la implementación captura su propia
excepción, la registra con `log.error` y continúa. Cambiar esta propagación requiere enmienda de
esta constitución.

El logging MUST usar SLF4J a través de `@Slf4j` de Lombok. `System.out.println` está prohibido.

**Rationale:** concentrar el mapeo de errores a HTTP en un único punto evita respuestas
inconsistentes entre endpoints. Externalizar los mensajes mantiene el sistema traducible. Y aislar
la transacción de auditoría garantiza que un fallo al registrar la bitácora nunca tumbe la carga
de un documento que ya se guardó.

### IX. Seguridad por Defecto y Protección de Datos (NO NEGOCIABLE)

Todo endpoint MUST tener una regla de autorización explícita para los roles `ADMIN`, `EDITOR` o
`READER`, ya sea con `@PreAuthorize` (a nivel de método o de clase) o en `SecurityConfig`.

`SecurityConfig` MUST terminar con `anyRequest().authenticated()`. Los endpoints públicos son
únicamente los enumerados de forma explícita antes de esa regla; hoy son `POST /auth/login`, la
documentación OpenAPI/Swagger, `/version` y `/actuator/health`. Añadir un endpoint a esa lista
MUST justificarse en el PR.

Los secretos (clave de firma JWT, credenciales de base de datos, credenciales de AWS) MUST NOT
estar en el repositorio ni en archivos `application*.yaml` versionados: se obtienen de variables
de entorno o de AWS Parameter Store.

Las contraseñas MUST almacenarse con un hash adaptativo (BCrypt o Argon2), nunca en claro ni con
un hash rápido.

Contraseñas, tokens JWT y contenido de documentos MUST NOT escribirse en los logs ni en los
mensajes de las excepciones.

Todo componente de un `record` con sufijo `RequestDto` MUST llevar al menos una anotación de
`jakarta.validation.constraints` que acote su valor (`@NotNull`, `@NotBlank`, `@Size`, `@Email`,
`@Pattern`, entre otras). En una colección, las restricciones declaradas sobre los elementos
(`List<@NotBlank String>`) cuentan como acotación de ese componente, y la colección MUST llevar
además `@Valid` para que se evalúen.

Un componente que admita `null` MUST marcarse con `@Nullable` (`jakarta.annotation.Nullable`), para
que la ausencia de `@NotNull` o `@NotBlank` sea una decisión declarada y no un olvido. `@Schema`
describe la API; no sustituye a la restricción ni al marcador de nulabilidad.

El parámetro del controlador MUST llevar `@Valid`. Ningún controlador MUST devolver una entidad
JPA: solo `record` de response.

**Rationale:** el sistema custodia documentos institucionales, algunos con datos personales de
estudiantes y familias. Un endpoint sin regla de autorización, o un secreto filtrado al
repositorio, es una brecha que no se corrige con una migración ni con un refactor. Es verificable:
se revisan los controladores en busca de endpoints sin regla, se busca en el historial de Git
cadenas de credenciales, se buscan llamadas a `log.*` con argumentos sensibles y se recorre cada
`RequestDto` comprobando que ningún componente quede sin restricción ni sin marcador de nulabilidad.

## Restricciones Técnicas

Estas restricciones describen el stack comprometido. Cambiarlas requiere enmienda.

- **Plataforma:** Java 17, Spring Boot 3.5.x, Maven con wrapper (`./mvnw`).
- **Persistencia:** PostgreSQL con Spring Data JPA. El esquema lo gobierna Flyway exclusivamente
  (Principio VI). Los enums se modelan en base de datos como `VARCHAR` + restricción `CHECK`, no
  como tipos enumerados nativos, para mantener legible el volcado de datos.
- **Mapeo:** MapStruct con `componentModel = "spring"`. Los mappers MUST declararse como **clases
  abstractas**, no interfaces, para poder incluir `@BeforeMapping` de validación null-safety y
  métodos concretos de agregación.
- **Módulo `document` — CQRS:** la escritura y la lectura están separadas en cinco interfaces
  (`DocumentCommandService`, `DocumentQueryService`, `DocumentSearchService`,
  `DocumentContentService`, `DocumentBatchService`). Una funcionalidad nueva de documentos MUST
  ubicarse en la que corresponda a su responsabilidad, no en la más cercana.
- **Almacenamiento de archivos:** tras la interfaz `FileStorageService`, con implementación local
  para desarrollo y S3 para entornos desplegados. El código de negocio MUST depender solo de la
  interfaz; ninguna clase fuera de `document/storage` debe saber cuál está activa.
- **Seguridad:** autenticación por JWT (`java-jwt`) con tres roles: `ADMIN`, `EDITOR`, `READER`.
- **Validación de archivos:** el tipo real se determina por contenido con Apache Tika, no por la
  extensión ni por el `Content-Type` declarado por el cliente.
- **Configuración:** el perfil `test` MUST quedar fijado en la ejecución de pruebas, para que una
  variable de entorno de la shell del desarrollador no haga que `verify` intente leer AWS
  Parameter Store.

## Flujo de Desarrollo y Puertas de Calidad

- **Puerta obligatoria:** `./mvnw clean verify` en verde, incluidos los umbrales de JaCoCo
  (Principio V). El workflow de CI la ejecuta en cada PR y MUST bloquear el merge si falla.
- **Revisión de PR:** la revisión MUST verificar explícitamente el cumplimiento de los principios
  de esta constitución, no solo que el código funcione. Una violación de un principio marcado
  NO NEGOCIABLE bloquea el merge.
- **Complejidad justificada:** toda desviación de la solución más simple MUST estar argumentada en
  la descripción del PR. Ante duda, gana la opción más simple.
- **Trabajo por feature:** rama por feature partiendo de `main`. El flujo de especificación
  (`/speckit-specify` → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement`) es la vía
  recomendada para funcionalidad nueva.
- **Pruebas de integración (SHOULD):** hoy no existen. Las migraciones Flyway y las consultas de
  repositorio (especialmente las nativas) no están cubiertas por ninguna prueba automatizada.
  Añadir esta capa es la mejora de calidad pendiente de mayor impacto; cuando se incorpore,
  MUST convertirse en principio por enmienda MINOR.
- **Convenciones de detalle:** el naming de clases, métodos, tablas y claves i18n, los niveles de
  log, y los patrones de mapper y de test viven en `CLAUDE.md`. No son parte de esta constitución
  y pueden cambiar sin enmienda.

## Gobernanza

Esta constitución prevalece sobre cualquier otra práctica, documento heredado o preferencia
personal. Cuando otra guía del repositorio la contradiga, manda la constitución y la otra guía se
corrige.

### Procedimiento de enmienda

Añadir, modificar o retirar un principio requiere estos pasos, en orden:

1. **Propuesta.** Abrir un PR que modifique únicamente `.specify/memory/constitution.md`. El
   cuerpo del PR debe explicar qué problema real motiva el cambio y qué regla concreta se podrá
   verificar después que hoy no se puede.
2. **Verificabilidad.** El principio propuesto debe redactarse con MUST / MUST NOT / SHOULD y ser
   comprobable en una revisión de código o por una herramienta. Si no se puede señalar qué archivo
   lo incumpliría, es una convención y va a `CLAUDE.md`, no aquí.
3. **Límite de tamaño.** El conjunto SHOULD mantenerse en un número que un revisor pueda tener en
   la cabeza durante un PR, en torno a diez. No hay un tope rígido: lo que se exige es que cada
   principio nuevo venga acompañado de la razón por la que ninguno de los existentes puede
   absorberlo. Una constitución que nadie recuerda no se cumple.
4. **Impacto.** Enumerar en el PR el código existente que quedaría en violación. Si hay
   violaciones, el PR debe incluir el plan: corregirlas en el mismo PR, o registrarlas como
   desviación con fecha objetivo (ver más abajo).
5. **Versionado.** Incrementar la versión según la política de abajo y actualizar la línea final
   del documento: `**Version**`, y `**Last Amended**` con la fecha de merge en formato
   `YYYY-MM-DD`. `**Ratified**` nunca cambia: es la fecha de adopción original.
6. **Sync Impact Report.** Encabezar el archivo con un comentario HTML que registre el cambio de
   versión, los principios añadidos, modificados o eliminados, y los TODO diferidos. Es material
   de revisión: se elimina antes de commitear la enmienda.
7. **Aprobación.** Merge del PR por el responsable del proyecto. Sin aprobación explícita no hay
   enmienda.

### Política de versionado

La constitución usa versionado semántico, independiente de la versión de la aplicación:

- **MAJOR** — se retira o se redefine un principio de forma incompatible con el código o el
  criterio anterior. Ejemplo: permitir el borrado físico, o abandonar la separación por feature.
- **MINOR** — se añade un principio o una sección, o se amplía materialmente una guía existente.
  Ejemplo: elevar las pruebas de integración de SHOULD a principio propio. **Este es el caso
  habitual al añadir principios nuevos.**
- **PATCH** — aclaraciones de redacción, correcciones de tipografía, precisión de un ejemplo. No
  cambia lo que está permitido ni lo que está prohibido.

### Revisión de cumplimiento

Todo PR MUST revisarse contra estos principios. La revisión de una violación tiene dos salidas
posibles: se corrige, o se registra como desviación con fecha objetivo de corrección. No existe la
tercera salida de ignorarla.

**Desviaciones registradas:**

- **D-4 — Dependencias cruzadas de repositorio entre `document`, `category` y `dashboard`
  (Principio I).** `DocumentCommandServiceImpl`, `DocumentBatchServiceImpl` y
  `DocumentSearchServiceImpl` inyectan `CategoryRepository` para resolver y validar categorías;
  `DashboardServiceImpl` inyecta `DocumentRepository` para sus conteos y para el top-10. En ambos
  casos un módulo usa el repositorio de otro en lugar de su interfaz de servicio. Aceptada como
  deuda técnica a corregir, **no** como excepción permanente. Atención al abordarla: inyectar
  `CategoryService` en el módulo `document` cierra un ciclo de Spring, porque `CategoryServiceImpl`
  ya inyecta `DocumentCommandService`; hay que romperlo extrayendo una interfaz de solo lectura de
  `category` que no dependa de `document`, no con `@Lazy`. Fecha objetivo: por definir.

D-1, D-2 y D-3 se retiran en esta enmienda: quedaron corregidas y verificadas antes de su fecha
objetivo.

### Regla de flujo de trabajo: changelog

Todo cambio con efecto visible para el usuario MUST registrarse en `CHANGELOG.md` en el mismo PR
que lo introduce:

- El formato sigue [Keep a Changelog](https://keepachangelog.com) y el proyecto se adhiere a
  [Versionado Semántico](https://semver.org).
- Las versiones son títulos de segundo nivel; los tipos de cambio, de tercer nivel.
- Los tipos permitidos son exactamente: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`,
  `Security`. No se inventan categorías nuevas.
- El título de versión sigue el formato `## [X.Y.Z] - YYYY-MM-DD`, donde la fecha es la de paso a
  producción. Lo aún no liberado va bajo `## [Unreleased]`.
- Cada entrada es una viñeta que empieza con un verbo en pasado o infinitivo, clara y concisa
  (ej. "Añadido sistema de autenticación JWT"), con el número de issue o PR al final si existe
  (ej. `(#42)`).
- La versión de la aplicación se incrementa según SemVer: `MAJOR` para cambios incompatibles,
  `MINOR` para funcionalidad compatible, `PATCH` para correcciones.

### Guía en tiempo de desarrollo

`CLAUDE.md` contiene las convenciones operativas del día a día: naming, niveles de log, patrones
de mapper y de test, y comandos habituales. Es complementario a esta constitución y subordinado a
ella.

**Version**: 1.0.1 | **Ratified**: 2026-09-19 | **Last Amended**: 2026-09-20
