**DocuRural**  | Historias de Usuario — Sprint 2

**DOCURURAL**

Sistema de Gestión Documental

**HISTORIAS DE USUARIO**

Sprint 2 — Gestión Documental

| **Proyecto**                 | DocuRural — Gestión Documental             |
|------------------------------|--------------------------------------------|
| **Documento**                | Historias de Usuario — Sprint 2            |
| **Versión**                  | 1.0                                        |
| **Fecha**                    | Abril 2026                                 |
| **Estado**                   | Definitivo                                 |
| **Sprint**                   | Sprint 2 — Gestión Documental              |
| **Requerimientos cubiertos** | RF-01, RF-02, RF-06                        |
| **Destinatario**             | IERD Miña y Ticha — Guachetá, Cundinamarca |

*Documento confidencial — Uso interno de la IERD Miña y Ticha*

# 1. Introducción

Este documento contiene las historias de usuario correspondientes al Sprint 2 del proyecto DocuRural. El Sprint 2 se
enfoca en la gestión documental: carga, clasificación, visualización y descarga de documentos.

Los requerimientos funcionales cubiertos son RF-01 (Registro y carga de documentos), RF-02 (Clasificación por
categorías) y RF-06 (Visualización y descarga de documentos). Los requerimientos RF-03 (Búsqueda) y RF-05 (Dashboard) se
abordan en el Sprint 3.

## 1.1 Alcance del Sprint 2

El Sprint 2 cubre las siguientes funcionalidades:

- Gestión completa de categorías documentales (crear, editar, desactivar, listar).

- Carga de documentos individuales y múltiples con metadatos obligatorios.

- Listado de documentos con información básica.

- Visualización en línea de archivos PDF e imágenes (JPG, PNG).

- Descarga de documentos en todos los formatos soportados.

- Edición de metadatos de documentos existentes.

- Eliminación lógica de documentos (borrado lógico, no físico).

- Registro de actividad en activity_log para todas las acciones documentales.

## 1.2 Dependencias del Sprint 1

El Sprint 2 depende de los siguientes entregables del Sprint 1:

- Autenticación JWT funcional (HU-01): toda acción del Sprint 2 requiere token válido.

- Control de roles (HU-06): ADMIN y EDITOR pueden cargar y editar; READER solo lee y descarga.

- Modelo de datos completo: tablas documents, categories y activity_log ya creadas con sus índices.

- Seed de categorías: las 8 categorías predefinidas ya existen en la base de datos.

- Log de actividad: acciones UPLOAD, DOWNLOAD, VIEW, EDIT_DOC, DELETE_DOC, CREATE_CATEGORY, EDIT_CATEGORY y
  DEACTIVATE_CATEGORY ya definidas en el modelo.

## 1.3 Tareas técnicas del Sprint 2 (sin historia de usuario)

Las siguientes actividades forman parte del Sprint 2 pero son tareas de infraestructura que no generan historias de
usuario. Se listan como referencia para el equipo de desarrollo:

- Configurar el módulo de almacenamiento de archivos en el backend: directorio base, subDirectorios por año/mes,
  permisos de escritura.

- Implementar el servicio de validación de archivos: tipo MIME real (no solo extensión), tamaño máximo (10 MB).

- Configurar Nginx para servir archivos estáticos desde el directorio de uploads con control de acceso.

- Implementar el visor PDF integrado en el frontend Angular (ng2-pdf-viewer o similar).

- Configurar los endpoints de la API para los módulos de Categorías (CAT-XX) y Documentos (DOC-XX).

# 2. Historias de Usuario — RF-02 (Categorías)

A continuación se detallan las 4 historias de usuario que cubren el requerimiento funcional RF-02: Clasificación por
categorías.

## HU-16: Creación de categoría

| **ID**            | HU-16    |
|-------------------|----------|
| **Requerimiento** | RF-02    |
| **Prioridad**     | Alta     |
| **Sprint**        | Sprint 2 |
| **Complejidad**   | Baja     |

Enunciado

*Como administrador del sistema,*

*quiero crear nuevas categorías documentales,*

*para ampliar la clasificación de documentos cuando la institución lo requiera.*

Criterios de aceptación

- Solo los usuarios con rol ADMIN pueden acceder a la funcionalidad de creación de categorías.

- El formulario solicita: nombre de la categoría (obligatorio) y descripción (opcional).

- Validaciones del nombre: mínimo 3, máximo 100 caracteres; no puede estar vacío.

- Si ya existe una categoría con el mismo nombre (activa o inactiva), el sistema muestra: "Ya existe una categoría con
  este nombre".

- La categoría se crea con estado ACTIVE y created_at con la fecha y hora actual.

- El campo created_by se establece automáticamente con el ID del administrador autenticado.

- Al crearse exitosamente, se muestra un mensaje de confirmación y la nueva categoría aparece en el listado.

- El sistema registra la acción CREATE_CATEGORY en activity_log.

- Si un usuario con rol EDITOR o READER intenta acceder, se muestra: "No tiene permisos para realizar esta acción".

**Endpoint: **POST /api/categories

**Notas técnicas: **Las 8 categorías predefinidas (seed) se tratan igual que cualquier categoría creada manualmente; no
tienen protección especial contra edición o desactivación.

## HU-17: Edición de categoría

| **ID**            | HU-17    |
|-------------------|----------|
| **Requerimiento** | RF-02    |
| **Prioridad**     | Alta     |
| **Sprint**        | Sprint 2 |
| **Complejidad**   | Baja     |

Enunciado

*Como administrador del sistema,*

*quiero editar el nombre y la descripción de una categoría existente,*

*para corregir errores o mejorar la descripción sin perder los documentos asociados.*

Criterios de aceptación

- Solo los usuarios con rol ADMIN pueden editar categorías.

- El formulario de edición muestra los datos actuales: nombre y descripción.

- Las validaciones de nombre son las mismas que en la creación (HU-16, criterio 3).

- Si se cambia el nombre por uno que ya existe en otra categoría (activa o inactiva), se muestra: "Ya existe una
  categoría con este nombre".

- Al guardar, los documentos ya asociados a la categoría mantienen su asociación con el nuevo nombre.

- Al guardarse exitosamente, se muestra un mensaje de confirmación y el listado se actualiza.

- El sistema registra la acción EDIT_CATEGORY en activity_log.

- No se puede editar una categoría con estado INACTIVE (el botón Editar no aparece para categorías inactivas).

**Endpoint: **PUT /api/categories/{id}

## HU-18: Desactivación de categoría

| **ID**            | HU-18    |
|-------------------|----------|
| **Requerimiento** | RF-02    |
| **Prioridad**     | Alta     |
| **Sprint**        | Sprint 2 |
| **Complejidad**   | Baja     |

Enunciado

*Como administrador del sistema,*

*quiero desactivar categorías que ya no están en uso,*

*para evitar que se asignen nuevos documentos a ellas sin eliminar los documentos existentes.*

Criterios de aceptación

- Solo los usuarios con rol ADMIN pueden desactivar categorías.

- Al hacer clic en "Desactivar", el sistema solicita confirmación con el mensaje: "¿Está seguro de desactivar la
  categoría '[nombre]'? No podrá usarse para clasificar nuevos documentos".

- Al confirmar, el campo status cambia de ACTIVE a INACTIVE.

- Las categorías desactivadas no aparecen en el selector del formulario de carga de documentos.

- Los documentos ya clasificados bajo la categoría desactivada permanecen accesibles y conservan su categoría.

- La categoría desactivada sigue apareciendo en el listado con una indicación visual clara (etiqueta "Inactiva").

- El administrador puede reactivar una categoría desactivada, cambiando su estado de vuelta a ACTIVE.

- No es posible eliminar categorías de la base de datos (no existe la opción de borrado físico).

- El sistema registra la acción DEACTIVATE_CATEGORY en activity_log.

**Endpoint: **PATCH /api/categories/{id}/status

**Body: **{ "status": "INACTIVE" } o { "status": "ACTIVE" } para reactivación.

## HU-19: Listado de categorías

| **ID**            | HU-19    |
|-------------------|----------|
| **Requerimiento** | RF-02    |
| **Prioridad**     | Alta     |
| **Sprint**        | Sprint 2 |
| **Complejidad**   | Baja     |

Enunciado

*Como administrador del sistema,*

*quiero ver el listado completo de categorías con su estado y cantidad de documentos asociados,*

*para gestionar la clasificación documental de la institución.*

Criterios de aceptación

- Solo los usuarios con rol ADMIN pueden acceder al listado de gestión de categorías.

- El listado muestra en tabla: nombre, descripción, cantidad de documentos asociados, estado (Activa/Inactiva) y fecha
  de creación.

- Las categorías inactivas se distinguen visualmente (texto en gris o etiqueta de estado diferenciada).

- Cada fila activa incluye las acciones: Editar y Desactivar. Las filas inactivas muestran solo: Activar.

- El listado se ordena alfabéticamente por nombre por defecto.

- Se muestra un contador con el total de categorías (ej. "10 categorías — 8 activas, 2 inactivas").

- Si no hay categorías registradas, se muestra: "No hay categorías registradas en el sistema".

- El listado es responsive: en móvil los datos se reorganizan sin scroll horizontal.

**Endpoint: **GET /api/categories

**Notas técnicas: **La cantidad de documentos asociados se obtiene como conteo agregado desde la tabla documents (campo
category_id), contando solo documentos con status = 'ACTIVE'. No se requiere paginación en el MVP dado el volumen
esperado de categorías.

# 3. Historias de Usuario — RF-01 y RF-06 (Documentos)

A continuación se detallan las 7 historias de usuario que cubren los requerimientos funcionales RF-01 (Registro y carga
de documentos) y RF-06 (Visualización y descarga de documentos).

## HU-09: Carga de un documento

| **ID**            | HU-09    |
|-------------------|----------|
| **Requerimiento** | RF-01    |
| **Prioridad**     | Alta     |
| **Sprint**        | Sprint 2 |
| **Complejidad**   | Media    |

Enunciado

*Como usuario con rol ADMIN o EDITOR,*

*quiero cargar un documento al repositorio con sus metadatos descriptivos,*

*para que quede registrado, clasificado y disponible para su consulta por el personal autorizado.*

Criterios de aceptación

- Solo los usuarios con rol ADMIN o EDITOR pueden cargar documentos.

- El formulario de carga solicita los siguientes campos: archivo (obligatorio), título del documento (obligatorio),
  categoría (obligatorio, selector con categorías activas), área responsable (obligatorio) y fecha del documento (
  obligatorio). La descripción es opcional.

- El sistema acepta únicamente los formatos: PDF, DOCX, XLSX, JPG y PNG.

- El tamaño máximo permitido por archivo es de 10 MB. Si se supera, se muestra: "El archivo supera el tamaño máximo
  permitido de 10 MB".

- El sistema valida el tipo MIME real del archivo (no solo la extensión) para evitar archivos maliciosos con extensión
  cambiada.

- El sistema valida que el título y la categoría no estén vacíos antes de permitir la carga.

- El selector de categorías solo muestra las categorías con estado ACTIVE.

- Al completar la carga exitosamente, el sistema confirma con un mensaje de éxito y muestra el documento en el listado.

- El sistema registra la acción UPLOAD en activity_log con user_id, document_id, action_timestamp e ip_address.

- Los campos uploaded_by y created_at se establecen automáticamente a partir del token JWT y la fecha/hora del servidor.

- Si un usuario con rol READER intenta cargar, el backend rechaza la petición con HTTP 403.

**Endpoint: **POST /api/documents (multipart/form-data)

**Notas técnicas: **El archivo se almacena en el sistema de archivos local bajo
/opt/docurural/uploads/documents/{año}/{mes}/{uuid}.{ext}. La ruta se persiste en el campo file_path de la tabla
documents. El nombre original del archivo se conserva en original_file_name.

## HU-10: Carga múltiple de documentos

| **ID**            | HU-10    |
|-------------------|----------|
| **Requerimiento** | RF-01    |
| **Prioridad**     | Alta     |
| **Sprint**        | Sprint 2 |
| **Complejidad**   | Media    |

Enunciado

*Como usuario con rol ADMIN o EDITOR,*

*quiero cargar hasta 5 documentos simultáneamente en una sola operación,*

*para agilizar la digitalización de documentos relacionados sin repetir el proceso de carga uno por uno.*

Criterios de aceptación

- El formulario permite seleccionar hasta 5 archivos simultáneamente desde el explorador de archivos del sistema
  operativo.

- Si se intenta seleccionar más de 5 archivos, el sistema muestra: "Solo puede cargar hasta 5 archivos a la vez" y
  permite al usuario ajustar la selección.

- Los metadatos (categoría y área responsable) se aplican de forma común a todos los archivos del lote. El título se
  asigna a partir del nombre original de cada archivo, pero el usuario puede editarlo antes de confirmar la carga.

- Se muestra una barra de progreso individual para cada archivo durante el proceso de carga.

- Si uno de los archivos falla (tamaño excedido, formato no soportado), el sistema informa el error específico para ese
  archivo sin interrumpir la carga de los demás.

- Al finalizar el lote, se muestra un resumen: cuántos archivos se cargaron exitosamente y cuántos fallaron.

- Cada archivo del lote genera su propio registro en la tabla documents y su propia entrada en activity_log con acción
  UPLOAD.

- Se aplican las mismas validaciones de formato y tamaño que en la carga individual (HU-09).

**Endpoint: **POST /api/documents/batch (multipart/form-data, array de archivos)

**Notas técnicas: **El backend procesa cada archivo de forma independiente. Si un archivo falla, los demás se persisten
normalmente. La respuesta incluye el resultado individual de cada archivo del lote.

## HU-11: Visualización de documento

| **ID**            | HU-11    |
|-------------------|----------|
| **Requerimiento** | RF-06    |
| **Prioridad**     | Alta     |
| **Sprint**        | Sprint 2 |
| **Complejidad**   | Baja     |

Enunciado

*Como usuario autenticado,*

*quiero visualizar el contenido de un documento directamente en el navegador,*

*para consultar su contenido sin necesidad de descargarlo ni abrir programas externos.*

Criterios de aceptación

- Todos los usuarios autenticados (ADMIN, EDITOR y READER) pueden visualizar documentos.

- Los archivos PDF se muestran en un visor integrado dentro del navegador con controles básicos de navegación (página
  anterior, página siguiente, número de página actual y zoom).

- Las imágenes JPG y PNG se muestran en un visor con zoom básico (acercar y alejar) y opción de ver a tamaño completo.

- Los archivos DOCX y XLSX no tienen visor integrado: se muestra su información de metadatos y únicamente el botón de
  descarga.

- Junto al visor se muestra la ficha completa del documento: título, descripción, categoría, área responsable, fecha del
  documento, formato, tamaño del archivo, usuario que lo subió y fecha de carga.

- El visor se abre en una vista dedicada o en un panel lateral, sin abandonar la lista de documentos.

- El sistema registra la acción VIEW en activity_log con user_id, document_id, action_timestamp e ip_address.

- Si el archivo no existe en el sistema de archivos (ruta inválida), se muestra: "El archivo no está disponible.
  Contacte al administrador".

**Endpoint: **GET /api/documents/{id}/view (retorna la URL firmada o el stream del archivo)

**Notas técnicas: **Los archivos se sirven a través de Nginx con autenticación controlada por el backend. El frontend
utiliza ng2-pdf-viewer (o librería equivalente) para el visor PDF. El registro VIEW se genera al abrir el visor, no al
descargar.

## HU-12: Descarga de documento

| **ID**            | HU-12    |
|-------------------|----------|
| **Requerimiento** | RF-06    |
| **Prioridad**     | Alta     |
| **Sprint**        | Sprint 2 |
| **Complejidad**   | Baja     |

Enunciado

*Como usuario autenticado,*

*quiero descargar un documento a mi equipo local,*

*para tener acceso a él sin conexión o compartirlo con personas que no tienen acceso al sistema.*

Criterios de aceptación

- Todos los usuarios autenticados (ADMIN, EDITOR y READER) pueden descargar documentos.

- El botón de descarga está visible y accesible en: la ficha del documento (vista de detalle), el visor de documentos y
  el listado de documentos.

- El archivo se descarga con su nombre original (campo original_file_name de la tabla documents).

- El sistema incluye el header Content-Disposition: attachment en la respuesta para forzar la descarga en lugar de abrir
  el archivo en el navegador.

- El sistema registra la acción DOWNLOAD en activity_log con user_id, document_id, action_timestamp e ip_address.

- Si el archivo no existe en el sistema de archivos, se muestra: "El archivo no está disponible. Contacte al
  administrador".

**Endpoint: **GET /api/documents/{id}/download

**Notas técnicas: **El backend hace stream del archivo con el header Content-Disposition: attachment; filename="
{original_file_name}". No se redirige directamente a la ruta del sistema de archivos para mantener el control de acceso.

## HU-13: Edición de metadatos de documento

| **ID**            | HU-13    |
|-------------------|----------|
| **Requerimiento** | RF-01    |
| **Prioridad**     | Alta     |
| **Sprint**        | Sprint 2 |
| **Complejidad**   | Media    |

Enunciado

*Como usuario con rol ADMIN o EDITOR,*

*quiero editar los metadatos de un documento cargado previamente,*

*para corregir errores en la clasificación, el título o cualquier otro campo descriptivo sin necesidad de volver a
cargar el archivo.*

Criterios de aceptación

- Los usuarios con rol ADMIN pueden editar los metadatos de cualquier documento.

- Los usuarios con rol EDITOR solo pueden editar los metadatos de documentos que ellos mismos cargaron (campo
  uploaded_by coincide con su ID).

- Los usuarios con rol READER no pueden editar metadatos. El botón de edición no aparece en su vista.

- Los campos editables son: título, descripción, categoría, área responsable y fecha del documento.

- El archivo físico no puede cambiarse en la edición. Si se requiere reemplazar el archivo, debe cargarse un documento
  nuevo.

- Las validaciones son las mismas que en la carga inicial: título y categoría obligatorios, categoría debe ser una
  categoría activa.

- Al guardar exitosamente, se muestra un mensaje de confirmación y la ficha del documento se actualiza.

- El sistema registra la acción EDIT_DOC en activity_log.

- Si un EDITOR intenta editar un documento de otro usuario, el backend retorna HTTP 403 con el mensaje: "No tiene
  permisos para editar este documento".

**Endpoint: **PUT /api/documents/{id}

**Notas técnicas: **El backend valida los permisos comparando el uploaded_by del documento con el ID del usuario del
token JWT. Los campos file_path, original_file_name, file_format, file_size_bytes, uploaded_by y created_at son
inmutables desde este endpoint.

## HU-14: Eliminación lógica de documento

| **ID**            | HU-14    |
|-------------------|----------|
| **Requerimiento** | RF-01    |
| **Prioridad**     | Alta     |
| **Sprint**        | Sprint 2 |
| **Complejidad**   | Baja     |

Enunciado

*Como administrador del sistema,*

*quiero eliminar documentos de forma lógica,*

*para retirarlos del repositorio activo sin perder la trazabilidad histórica de las acciones realizadas sobre ellos.*

Criterios de aceptación

- Solo los usuarios con rol ADMIN pueden eliminar documentos.

- Al hacer clic en "Eliminar", el sistema solicita confirmación con el mensaje: "¿Está seguro de eliminar el
  documento '[título]'? Esta acción no se puede deshacer desde la interfaz".

- Al confirmar, el campo status del documento cambia de ACTIVE a DELETED (borrado lógico).

- Los documentos eliminados no aparecen en el listado principal ni en los resultados de búsqueda.

- Los registros en activity_log que referencian el documento eliminado se conservan íntegramente para garantizar la
  trazabilidad.

- El archivo físico en el sistema de archivos local no se elimina durante el borrado lógico del MVP.

- No existe la opción de eliminar documentos de la base de datos (borrado físico).

- El sistema registra la acción DELETE_DOC en activity_log.

- Los usuarios con rol EDITOR y READER no ven la opción de eliminar documentos.

**Endpoint: **DELETE /api/documents/{id}

**Notas técnicas: **El endpoint cambia el campo status a 'DELETED'. El archivo físico se retiene en el sistema de
archivos. La limpieza de archivos físicos sin referencia activa se gestionará como tarea de mantenimiento en versiones
posteriores.

## HU-15: Listado de documentos

| **ID**            | HU-15        |
|-------------------|--------------|
| **Requerimiento** | RF-01, RF-06 |
| **Prioridad**     | Alta         |
| **Sprint**        | Sprint 2     |
| **Complejidad**   | Media        |

Enunciado

*Como usuario autenticado,*

*quiero ver un listado de los documentos disponibles en el repositorio con su información básica,*

*para identificar rápidamente los documentos que necesito y acceder a ellos.*

Criterios de aceptación

- Todos los usuarios autenticados (ADMIN, EDITOR y READER) pueden acceder al listado de documentos.

- El listado muestra en tabla: título, categoría, área responsable, fecha del documento, formato (con ícono diferenciado
  por tipo), tamaño del archivo y usuario que lo subió.

- Cada fila incluye las acciones disponibles según el rol del usuario: Ver, Descargar (todos los roles); Editar (ADMIN y
  EDITOR propietario); Eliminar (solo ADMIN).

- El listado muestra únicamente documentos con status = ACTIVE.

- El listado está paginado: se muestran 20 documentos por página con controles de navegación (primera, anterior,
  siguiente, última página).

- El listado se ordena por fecha de carga descendente (más recientes primero) por defecto.

- Se muestra un contador con el total de documentos (ej. "47 documentos").

- Si no hay documentos cargados, se muestra: "No hay documentos en el repositorio. Comience cargando el primer
  documento".

- El listado es responsive: en dispositivos móviles se reorganiza la información de forma legible.

**Endpoint: **GET /api/documents

**Notas técnicas: **Este endpoint es la base para el motor de búsqueda del Sprint 3 (RF-03). En el Sprint 2 solo
devuelve todos los documentos activos paginados. Los parámetros de filtro se agregarán en el Sprint 3. El campo de
tamaño se formatea en el frontend: bytes < 1024 → B; < 1048576 → KB; resto → MB.

# 4. Matriz de Permisos por Rol — Sprint 2

La siguiente matriz define los permisos de cada rol sobre las funcionalidades del Sprint 2, como referencia
complementaria de la HU-06 (Sprint 1).

| **Funcionalidad**                   | **ADMIN** | **EDITOR** | **READER** |
|-------------------------------------|-----------|------------|------------|
| Crear categorías                    | ✅         | ❌          | ❌          |
| Editar categorías                   | ✅         | ❌          | ❌          |
| Desactivar/activar categorías       | ✅         | ❌          | ❌          |
| Ver listado de categorías (gestión) | ✅         | ❌          | ❌          |
| Cargar documentos (individual)      | ✅         | ✅          | ❌          |
| Cargar documentos (múltiple)        | ✅         | ✅          | ❌          |
| Ver listado de documentos           | ✅         | ✅          | ✅          |
| Visualizar documento (visor)        | ✅         | ✅          | ✅          |
| Descargar documento                 | ✅         | ✅          | ✅          |
| Editar metadatos (propios)          | ✅         | ✅          | ❌          |
| Editar metadatos (ajenos)           | ✅         | ❌          | ❌          |
| Eliminar documento (lógico)         | ✅         | ❌          | ❌          |

# 5. Resumen y Trazabilidad

La siguiente tabla resume todas las historias de usuario del Sprint 2, su requerimiento asociado, prioridad, complejidad
y la acción que registran en activity_log.

| **HU** | **Nombre**                   | **RF**       | **Prioridad** | **Complejidad** | **Acción en activity_log** |
|--------|------------------------------|--------------|---------------|-----------------|----------------------------|
| HU-16  | Creación de categoría        | RF-02        | Alta          | Baja            | CREATE_CATEGORY            |
| HU-17  | Edición de categoría         | RF-02        | Alta          | Baja            | EDIT_CATEGORY              |
| HU-18  | Desactivación de categoría   | RF-02        | Alta          | Baja            | DEACTIVATE_CATEGORY        |
| HU-19  | Listado de categorías        | RF-02        | Alta          | Baja            | — (consulta)               |
| HU-09  | Carga de un documento        | RF-01        | Alta          | Media           | UPLOAD                     |
| HU-10  | Carga múltiple de documentos | RF-01        | Alta          | Media           | UPLOAD (×n)                |
| HU-11  | Visualización de documento   | RF-06        | Alta          | Baja            | VIEW                       |
| HU-12  | Descarga de documento        | RF-06        | Alta          | Baja            | DOWNLOAD                   |
| HU-13  | Edición de metadatos         | RF-01        | Alta          | Media           | EDIT_DOC                   |
| HU-14  | Eliminación lógica           | RF-01        | Alta          | Baja            | DELETE_DOC                 |
| HU-15  | Listado de documentos        | RF-01, RF-06 | Alta          | Media           | — (consulta)               |

## 5.1 Cobertura de requerimientos

Las 11 historias de usuario cubren todos los criterios de aceptación de los tres requerimientos funcionales del Sprint
2:

- RF-01 — Registro y carga de documentos: HU-09 (carga individual), HU-10 (carga múltiple con barra de progreso),
  HU-13 (edición de metadatos), HU-14 (eliminación lógica), HU-15 (listado).

- RF-02 — Clasificación por categorías: HU-16 (crear), HU-17 (editar), HU-18 (desactivar/activar), HU-19 (listar con
  contador de documentos asociados).

- RF-06 — Visualización y descarga: HU-11 (visor PDF e imágenes, metadatos junto al visor), HU-12 (descarga con nombre
  original), HU-15 (botón de acciones en listado).

# 6. Historial de Versiones

| **Versión** | **Fecha**  | **Autor**          | **Cambios**                                                                                    |
|-------------|------------|--------------------|------------------------------------------------------------------------------------------------|
| 1.0         | Abril 2026 | Analista funcional | Versión definitiva — HU completas del Sprint 2 (RF-01, RF-02, RF-06). 11 historias de usuario. |

Confidencial — IERD Miña y Ticha, Guachetá | Página