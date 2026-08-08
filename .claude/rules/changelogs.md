# Reglas para el Changelog y Control de Versiones

Siempre que realices cambios en el proyecto o el usuario te pida documentar una nueva versión, debes seguir
estrictamente estas instrucciones para actualizar el archivo `CHANGELOG.md`.

## 1. Formato del Archivo

- Usa el estándar de [Keep a Changelog](https://keepachangelog.com).
- El archivo debe estructurarse con títulos de segundo nivel (`##`) para las versiones y títulos de tercer nivel (`###`)
  para los tipos de cambios.

## 2. Tipos de Cambios Permitidos

Agrupa los cambios estrictamente bajo estas categorías:

- `### Added`: Para nuevas características.
- `### Changed`: Para cambios en funcionalidades existentes.
- `### Deprecated`: Para características que se eliminarán pronto.
- `### Removed`: Para características eliminadas.
- `### Fixed`: Para correcciones de errores (bug fixes).
- `### Security`: En caso de vulnerabilidades de seguridad.

## 3. Formato de la Versión y Fecha

- Sigue el versionado semántico [Semantic Versioning (SemVer)](https://semver.org): `MAJOR.MINOR.PATCH`.
- Incrementa `MAJOR` para cambios incompatibles, `MINOR` para nuevas funcionalidades compatibles, y `PATCH` para
  correcciones.
- El formato del título de la versión debe ser: `## [X.Y.Z] - YYYY-MM-DD`. Por ejemplo: `## - 2026-08-07`.
- Si una versión aún no se ha liberado, usa: `## [Unreleased]`.

## 4. Estilo de Redacción

- Describe cada cambio en una lista con viñetas (`-`).
- Cada viñeta debe empezar con un verbo en pasado o infinitivo, de forma clara y concisa (ej. "Añadido sistema de
  autenticación JWT").
- Incluye el número de issue o pull request al final de la línea si está disponible (ej. `(#42)`).
