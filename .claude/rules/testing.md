---
paths:
  - "src/test/**"
---

# Convenciones de Pruebas

## Estructura por tipo de test

### Capa web — `@WebMvcTest`

Un test por controlador. Configuración obligatoria:

```java
@WebMvcTest(controllers = DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
// Excluir filtros de seguridad:
@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, JwtAuthenticationFilter.class})
```

- Mockear servicios con `@MockitoBean`.
- Nunca usar `@SpringBootTest` para pruebas de controlador.

### Capa servicio — `@ExtendWith(MockitoExtension.class)`

Pruebas unitarias puras; nunca levantar contexto Spring.

```java

@ExtendWith(MockitoExtension.class)
class DocumentCommandServiceTest {
    @Mock
    DocumentRepository documentRepository;
    @Spy
    DocumentMapper documentMapper = Mappers.getMapper(DocumentMapper.class);
    @InjectMocks
    DocumentCommandServiceImpl service;
}
```

### Mappers

Instanciar directamente con `Mappers.getMapper(XyzMapper.class)`. Verificar que campos sensibles no se expongan (ej:
`passwordHash` ausente en `UserResponse`).

## Nombrado de tests

Formato obligatorio: `should_<comportamientoEsperado>_when_<condición>`

```java

@Test
void should_returnToken_when_credentialsAreValid() { ...}

@Test
void should_throwResourceNotFoundException_when_documentIdDoesNotExist() { ...}

@Test
void should_forbidEdit_when_editorIsNotOwner() { ...}
```

## Fixtures y builders

- Usar **siempre** los builders de `TestFixtures`:
  `src/test/java/co/edu/docurural/support/TestFixtures.java`
- No instanciar objetos de dominio con `new` directamente en los tests.
- Ejemplos de métodos disponibles: `userAdmin(id)`, `userEditor(id)`, `categoryActive(id, name)`,
  `uploadDocumentRequest(categoryId)`.

## Constantes de prueba

Declarar constantes de clase para datos repetidos:

```java
private static final Long ACTOR_ID = 10L;
private static final AuditContext AUDIT = new AuditContext(ACTOR_ID, "127.0.0.1");
```

## @Spy en mappers dentro de tests de servicio

Usar `@Spy` con instancia real del mapper para ejecutar la lógica de mapeo real sin mockear:

```java

@Spy
DocumentMapper documentMapper = Mappers.getMapper(DocumentMapper.class);
```

## JaCoCo — clases excluidas de cobertura

Las siguientes clases están excluidas del umbral del 80% (configurado en `pom.xml`):

```
**/mapper/**
**/shared/config/**
**/shared/security/SecurityConstants
**/shared/security/JwtProperties
**/shared/security/CustomUserPrincipal
**/audit/**
**/util/**
**/dto/**
**/entity/**
**/enums/**
**/repository/**
BusinessRuleException
ConflictException
ResourceNotFoundException
FileStorageException
DocururalBackendApplication
```

Umbral: **≥80% cobertura de líneas (BUNDLE)**. Verificar con `./mvnw clean verify`.
