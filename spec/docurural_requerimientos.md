**DocuRural**    Documento de Requerimientos — MVP v1.0

**DOCURURAL**

Sistema de Gestión Documental

**DOCUMENTO DE REQUERIMIENTOS**

MVP — Versión 1.0

| **Proyecto**     | DocuRural — Gestión Documental             |
|------------------|--------------------------------------------|
| **Versión**      | 1.0                                        |
| **Fecha**        | Abril 2026                                 |
| **Estado**       | Borrador revisado (stack actualizado)      |
| **Destinatario** | IERD Miña y Ticha — Guachetá, Cundinamarca |

*Documento confidencial — Uso interno de la IERD Miña y Ticha*

# Tabla de Contenido

# 1. Introducción

## 1.1 Propósito del documento

Este documento define los requerimientos funcionales y no funcionales para el Producto Mínimo Viable (MVP) del sistema
DocuRural, una plataforma de gestión documental diseñada para la Institución Educativa Rural Departamental Miña y Ticha,
ubicada en la Vereda Miña del municipio de Guachetá, Cundinamarca. Su objetivo es servir como guía técnica y funcional
para el equipo de desarrollo.

## 1.2 Alcance del MVP

El MVP se enfoca en resolver el problema más crítico de la IERD Miña y Ticha: la digitalización, organización y acceso
controlado a los documentos institucionales de sus nueve sedes. No incluye flujos de aprobación, plantillas ni
funcionalidad offline, los cuales se abordarán en versiones posteriores.

## 1.3 Contexto y justificación

La IERD Miña y Ticha es una institución de carácter oficial, mixta, calendario A, que ofrece los niveles de Preescolar,
Básica Primaria, Básica Secundaria y Media Técnica con especialidad en Biotecnología en Tejido Vegetal (NIT
900.084.754-1, Código DANE 225317000172). Opera en nueve sedes rurales, atiende a 517 estudiantes y 1.599 familias,
impactando a 4.615 habitantes de la zona. Como institución rural enfrenta desafíos particulares en la gestión de sus
documentos: archivos físicos vulnerables al deterioro, búsqueda lenta de información, dependencia de una sola persona
que conoce la ubicación de los documentos y dificultad para cumplir con requerimientos de auditorías y rendición de
cuentas. DocuRural busca resolver estos problemas con una solución liviana, intuitiva y adaptada a las condiciones
tecnológicas del entorno rural de Guachetá.

## 1.4 Usuarios objetivo

| **Rol**            | **Perfil**                                    | **Necesidad principal**                                                           |
|--------------------|-----------------------------------------------|-----------------------------------------------------------------------------------|
| **Rector(a)**      | Máxima autoridad de la institución            | Supervisión general, acceso total, reportes de estado                             |
| **Secretario(a)**  | Gestión administrativa diaria                 | Carga, clasificación y búsqueda rápida de documentos                              |
| **Docente**        | Personal académico                            | Consulta de documentos pedagógicos e institucionales                              |
| **Coordinador(a)** | Líder de área o del programa de biotecnología | Acceso a documentos de su área, carga de informes y documentación del laboratorio |

# 2. Requerimientos Funcionales

## 2.1 RF-01: Registro y carga de documentos

**Descripción:**

El sistema debe permitir a los usuarios autorizados subir documentos digitales al repositorio central, asociando
metadatos descriptivos que faciliten su posterior localización y clasificación.

**Criterios de aceptación:**

- El usuario puede cargar archivos en formatos PDF, DOCX, XLSX, JPG y PNG.

- El tamaño máximo permitido por archivo es de 10 MB.

- Al momento de la carga, el sistema solicita: título del documento, categoría, área responsable, fecha del documento y
  descripción opcional.

- El sistema valida que el título y la categoría no estén vacíos antes de permitir la carga.

- Se permite la carga múltiple (hasta 5 archivos simultáneos).

- El sistema muestra una barra de progreso durante la carga.

- Al completar la carga, se confirma con un mensaje de éxito y se muestra el documento en el listado.

**Prioridad: Alta | Complejidad: Media**

## 2.2 RF-02: Clasificación por categorías

**Descripción:**

El sistema debe organizar los documentos en categorías predefinidas que reflejen la estructura documental de la IERD
Miña y Ticha, permitiendo al administrador gestionar dichas categorías.

**Categorías predefinidas:**

| **Categoría**       | **Descripción**                                                                           |
|---------------------|-------------------------------------------------------------------------------------------|
| **Actas**           | Actas de reuniones, consejos directivos, comités                                          |
| **Resoluciones**    | Resoluciones rectorales y administrativas                                                 |
| **Matrículas**      | Documentos de inscripción y registro de estudiantes                                       |
| **Certificados**    | Constancias de estudio, certificados de notas, diplomas                                   |
| **Correspondencia** | Comunicados oficiales enviados y recibidos                                                |
| **Informes**        | Informes pedagógicos, académicos, de gestión y del programa de biotecnología              |
| **Normatividad**    | Manuales de convivencia, PEI, planes de área, protocolos del laboratorio de biotecnología |
| **Otro**            | Documentos que no corresponden a ninguna categoría anterior                               |

**Criterios de aceptación:**

- Todo documento debe estar asociado obligatoriamente a una categoría.

- El administrador puede crear, editar y desactivar categorías (no eliminar, para preservar la integridad referencial).

- Las categorías desactivadas no aparecen en el formulario de carga pero sus documentos permanecen accesibles.

**Prioridad: Alta | Complejidad: Baja**

## 2.3 RF-03: Búsqueda y filtrado de documentos

**Descripción:**

El sistema debe proporcionar mecanismos rápidos y eficientes para localizar documentos, combinando búsqueda por texto
libre con filtros estructurados.

**Criterios de aceptación:**

- Búsqueda por texto libre que consulte título, descripción y nombre del archivo.

- Filtros combinables por: categoría, área responsable, rango de fechas y usuario que subió el documento.

- Los resultados se muestran en una tabla paginada (20 resultados por página) con: título, categoría, fecha, responsable
  y acciones disponibles.

- El tiempo de respuesta de la búsqueda no debe superar los 3 segundos.

- Se muestra un contador con el número total de resultados encontrados.

- Los filtros aplicados son visibles y se pueden limpiar individualmente o todos a la vez.

**Prioridad: Alta | Complejidad: Media**

## 2.4 RF-04: Gestión de usuarios y permisos

**Descripción:**

El sistema debe implementar un esquema de autenticación y autorización basado en roles para controlar el acceso a los
documentos y funcionalidades según el perfil de cada usuario.

**Roles del sistema:**

| **Rol**           | **Permisos**                                                                                          | **Restricciones**                                                |
|-------------------|-------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| **Administrador** | Crear, leer, editar, eliminar documentos. Gestionar usuarios, categorías y configuración del sistema. | Ninguna                                                          |
| **Editor**        | Crear y leer documentos. Editar sus propios documentos.                                               | No puede eliminar documentos ni gestionar usuarios o categorías. |
| **Lector**        | Leer y descargar documentos.                                                                          | No puede crear, editar ni eliminar documentos.                   |

**Criterios de aceptación:**

- Inicio de sesión con usuario y contraseña. La contraseña debe tener mínimo 8 caracteres.

- El administrador puede crear, editar y desactivar cuentas de usuario.

- Cada usuario tiene asignado un único rol.

- Las acciones no autorizadas muestran un mensaje claro de permiso denegado.

- La sesión expira tras 30 minutos de inactividad.

- Se registra la fecha y hora del último acceso de cada usuario.

**Prioridad: Alta | Complejidad: Media**

## 2.5 RF-05: Panel de control (Dashboard)

**Descripción:**

El sistema debe presentar una vista de resumen al iniciar sesión que permita al usuario tener una visión general del
estado del repositorio documental y acceder rápidamente a las acciones más frecuentes.

**Criterios de aceptación:**

- Tarjetas de resumen con: total de documentos, documentos subidos este mes, documentos por categoría, últimos 10
  documentos cargados.

- Accesos directos a: subir nuevo documento, buscar documentos, gestionar usuarios (solo administrador).

- Gráfico simple de distribución de documentos por categoría (gráfico de barras o dona).

- El dashboard se adapta al rol del usuario: el lector no ve opciones de carga ni gestión.

- El dashboard carga completamente en menos de 5 segundos.

**Prioridad: Media | Complejidad: Media**

## 2.6 RF-06: Visualización y descarga de documentos

**Descripción:**

El sistema debe permitir a los usuarios visualizar documentos directamente en el navegador (cuando el formato lo
permita) y descargarlos a su equipo local.

**Criterios de aceptación:**

- Los archivos PDF se visualizan en un visor integrado dentro del navegador.

- Las imágenes (JPG, PNG) se muestran en un visor con zoom básico.

- Los archivos DOCX y XLSX se ofrecen únicamente para descarga.

- Se muestra la información completa del documento (metadatos) junto al visor.

- El botón de descarga está disponible para todos los formatos.

- Se registra cada descarga en el log de actividad (usuario, documento, fecha).

**Prioridad: Alta | Complejidad: Baja**

# 3. Requerimientos No Funcionales

## 3.1 Rendimiento

- Tiempo de carga inicial de la aplicación: menor a 4 segundos en conexión de 5 Mbps.

- Búsquedas con respuesta en menos de 3 segundos para repositorios de hasta 10,000 documentos.

- Carga de archivos de hasta 10 MB sin timeout.

## 3.2 Usabilidad

Dado que los usuarios pueden tener poca experiencia con herramientas digitales, la interfaz debe ser excepcionalmente
simple:

- Navegación con máximo 3 clics para llegar a cualquier funcionalidad.

- Iconos acompañados de texto descriptivo en todos los botones.

- Mensajes de error claros y en lenguaje no técnico.

- Diseño responsivo que funcione correctamente en: escritorio (resolución mínima 1024x768), tableta y teléfono móvil.

- Tamaño de fuente base de 16px para facilitar la lectura.

## 3.3 Seguridad

- Autenticación obligatoria para acceder a cualquier recurso del sistema.

- Contraseñas almacenadas con hash (BCryptPasswordEncoder de Spring Security).

- Comunicación cifrada mediante HTTPS.

- Protección contra inyección SQL, XSS y CSRF.

- Sesión con token JWT con expiración configurable.

## 3.4 Compatibilidad

El sistema debe funcionar correctamente en los siguientes navegadores y sus dos versiones más recientes: Google Chrome,
Mozilla Firefox, Microsoft Edge, Safari. También debe ser funcional en dispositivos móviles con Android 10 o superior e
iOS 14 o superior.

## 3.5 Disponibilidad y respaldo

- Disponibilidad objetivo del 95% (considerando las limitaciones de infraestructura rural).

- Respaldo automático de la base de datos cada 24 horas.

- Los archivos se almacenan con redundancia (al menos una copia adicional).

# 4. Arquitectura Propuesta

Se propone una arquitectura liviana y de fácil despliegue, considerando las restricciones técnicas del entorno rural:

## 4.1 Stack tecnológico

| **Capa**           | **Tecnología**            | **Justificación**                                                                |
|--------------------|---------------------------|----------------------------------------------------------------------------------|
| **Frontend**       | Angular                   | Estructura modular y robusta, ideal para escalar hacia versiones futuras         |
| **Backend**        | Java 17 + Spring Boot     | Robusto, maduro, con Spring Security integrado para autenticación y autorización |
| **Base de datos**  | PostgreSQL                | Robusta, gratuita, excelente para búsquedas de texto                             |
| **Almacenamiento** | Sistema de archivos local | Simple, sin dependencias externas, fácil respaldo                                |
| **Autenticación**  | JWT                       | Sin estado, integración nativa con Spring Security                               |
| **Servidor**       | Linux (Ubuntu)            | Estable, gratuito, bajo consumo de recursos                                      |

## 4.2 Modelo de despliegue

Para el MVP se recomienda un despliegue en un servidor local dentro de la sede principal de la IERD Miña y Ticha en la
Vereda Miña (un equipo dedicado o reutilizado con JDK 17 instalado), accesible a través de la red local. El backend se
despliega como un archivo JAR ejecutable de Spring Boot y el frontend Angular se sirve como archivos estáticos desde
Nginx. Esto elimina la dependencia de conectividad a internet para el uso cotidiano. Considerando que la institución
cuenta con nueve sedes rurales, opcionalmente se puede configurar acceso remoto mediante VPN o un servicio de túnel para
consultas desde las sedes alternas.

## 4.3 Modelo de datos simplificado

Las entidades principales del MVP son:

- **Usuarios: **id, nombre, email, contraseña (hash), rol, estado, fecha de creación, último acceso

- **Documentos: **id, título, descripción, categoría, área, ruta del archivo, tamaño, formato, usuario que lo subió,
  fecha de carga

- **Categorías: **id, nombre, descripción, estado (activa/inactiva)

- **Log de actividad: **id, usuario, acción, documento afectado, fecha y hora, dirección IP

# 5. Restricciones y Supuestos

## 5.1 Restricciones

- La conectividad a internet puede ser intermitente o de baja velocidad (menos de 10 Mbps).

- Los equipos disponibles pueden ser de gama baja (procesadores Celeron o similares, 4 GB de RAM).

- El presupuesto para infraestructura tecnológica es limitado; se privilegian soluciones gratuitas y de código abierto.

- No se dispone de personal de TI dedicado en la IERD Miña y Ticha.

## 5.2 Supuestos

- La IERD Miña y Ticha cuenta con al menos un equipo en la sede principal (Vereda Miña) que puede funcionar como
  servidor local (mínimo 4 GB de RAM, recomendado 8 GB, con JDK 17 instalado).

- Existe una red local (LAN o WiFi) funcional dentro de la sede principal en la Vereda Miña.

- La rectoría designará al menos una persona (preferiblemente el secretario o secretaria) como administrador del
  sistema, quien recibirá capacitación básica.

- Los documentos a digitalizar están en buen estado o ya cuentan con copias digitales (escáner o fotografía).

- Se realizará una carga inicial de documentos históricos como parte de la implementación.

# 6. Criterios de Éxito del MVP

El MVP se considerará exitoso si cumple los siguientes indicadores medibles dentro de los primeros 30 días de uso:

| **#** | **Indicador**                                  | **Meta**                | **Método de medición** |
|-------|------------------------------------------------|-------------------------|------------------------|
| 1     | Documentos digitalizados y cargados al sistema | **≥ 100 documentos**    | Contador del sistema   |
| 2     | Usuarios activos usando el sistema             | **≥ 80% del personal**  | Log de acceso          |
| 3     | Tiempo promedio para localizar un documento    | **<**** 1 minuto**      | Prueba con usuarios    |
| 4     | Satisfacción de los usuarios                   | **≥ 4/5 en encuesta**   | Encuesta de usabilidad |
| 5     | Incidentes críticos del sistema                | **0 pérdidas de datos** | Log del sistema        |
| 6     | Disponibilidad del sistema                     | **≥ 95%**               | Monitoreo de uptime    |

# 7. Plan de Implementación del MVP

El desarrollo del MVP se organiza en 4 sprints de 2 semanas cada uno (8 semanas en total):

| **Sprint**   | **Enfoque**          | **Entregables**                                                                           | **Duración** |
|--------------|----------------------|-------------------------------------------------------------------------------------------|--------------|
| **Sprint 1** | Infraestructura base | Configuración del entorno, base de datos, autenticación y gestión de usuarios             | Semanas 1–2  |
| **Sprint 2** | Gestión documental   | Carga, clasificación, visualización y descarga de documentos                              | Semanas 3–4  |
| **Sprint 3** | Búsqueda y dashboard | Motor de búsqueda con filtros, panel de control con estadísticas                          | Semanas 5–6  |
| **Sprint 4** | Pruebas y despliegue | Pruebas de integración, corrección de errores, despliegue en servidor local, capacitación | Semanas 7–8  |

# 8. Visión de Versiones Futuras

Las siguientes funcionalidades están planificadas para versiones posteriores al MVP y no forman parte del alcance de
este documento:

## 8.1 Versión 2.0 — Procesos y trazabilidad

- Flujo de aprobación de documentos (borrador, revisión, aprobado, archivado)

- Visto bueno digital con registro de aprobador y fecha

- Plantillas predefinidas para documentos recurrentes

- Control de versiones con historial de cambios

- Log de auditoría detallado

- Notificaciones internas del sistema

## 8.2 Versión 3.0 — Resiliencia rural

- Funcionamiento offline con sincronización posterior

- Digitalización desde cámara del celular

- Políticas de retención y alertas de vencimiento

- Generación de reportes avanzados

- Respaldo automático programado

- Exportación masiva de documentos

# 9. Glosario

| **Término**    | **Definición**                                                                    |
|----------------|-----------------------------------------------------------------------------------|
| **MVP**        | Producto Mínimo Viable. Versión inicial con las funcionalidades esenciales.       |
| **Metadatos**  | Información descriptiva asociada a un documento (título, fecha, categoría, etc.). |
| **JWT**        | JSON Web Token. Estándar para autenticación basada en tokens.                     |
| **CRUD**       | Crear, Leer, Actualizar, Eliminar. Operaciones básicas sobre datos.               |
| **Responsivo** | Diseño que se adapta automáticamente a diferentes tamaños de pantalla.            |
| **Hash**       | Transformación criptográfica irreversible utilizada para proteger contraseñas.    |
| **PEI**        | Proyecto Educativo Institucional.                                                 |
| **Log**        | Registro cronológico de actividades realizadas en el sistema.                     |

*Fin del documento*

	Confidencial — IERD Miña y Ticha, Guachetá	Página