[← Volver al README](../README.md)

# Pruebas

```bash
# Ejecutar todas las pruebas
./mvnw clean test

# Ejecutar una clase de prueba específica
./mvnw test -Dtest=AuthControllerWebMvcTest

# Ejecutar con reporte de cobertura JaCoCo (exige LINE ≥ 80% y BRANCH ≥ 65%)
./mvnw clean verify
```

El reporte HTML de cobertura se genera en:

```
target/site/jacoco/index.html
```

La suite bajo `src/test/java` no usa `@SpringBootTest` ni Testcontainers (no requiere base de datos), y combina tres
tipos de prueba:

| Tipo                          | Ejemplo                                              |
|--------------------------------|----------------------------------------------------------|
| `@WebMvcTest` (capa web)         | `DocumentControllerWebMvcTest`                              |
| Unitarias con Mockito              | `DocumentCommandServiceTest`                                   |
| Puras (mappers/validadores)           | `DocumentMapperTest`, `PasswordsMatchValidatorTest`                |
