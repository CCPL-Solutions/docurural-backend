# Clean Code y Principios SOLID

## SOLID

**S — Single Responsibility**
Cada clase tiene una sola razón para cambiar.
Un Service orquesta un caso de uso. Un Mapper solo mapea. Un Validator solo valida.
Si una clase necesita más de 2-3 colaboradores, probablemente está haciendo demasiado.

**O — Open/Closed**
Extender comportamiento sin modificar código existente.
Preferir Strategy o decoradores sobre if/switch que crecen con cada nuevo caso.

**L — Liskov Substitution**
Las implementaciones de un puerto deben ser intercambiables sin que el servicio lo note.
Si un test del Service necesita saber qué implementación de Port está usando, el principio está roto.

**I — Interface Segregation**
Puertos pequeños y enfocados. Si un adaptador implementa un Port pero deja métodos vacíos
o lanza UnsupportedOperationException, el Port es demasiado grande — dividirlo.

**D — Dependency Inversion**
Los servicios dependen de interfaces (Port), nunca de implementaciones concretas.
Esto ya está garantizado por la arquitectura hexagonal — no romperlo.

## Nombres

- Los nombres deben revelar intención: `calculateTotalWithDiscount()`, no `calc()` ni `doStuff()`.
- Evitar encodings y prefijos: no `IProductPort`, no `strName`, no `objProduct`.
- Clases: sustantivos (`OrderValidator`). Métodos: verbos (`validateOrder`).
- Booleanos: prefijo `is`, `has`, `can` (`isActive`, `hasStock`, `canBeDeleted`).
- No usar abreviaciones salvo las ya establecidas en el proyecto (`dto`, `id`, `sku`).

## Funciones y métodos

- Un método hace una sola cosa. Si su nombre necesita "y" o "también", dividirlo.
- Máximo 3 parámetros. Más de 3: agrupar en un objeto o record.
- Sin flags booleanos como parámetro — crean bifurcaciones ocultas. Preferir dos métodos.
- Nivel de abstracción uniforme: un método no mezcla lógica de negocio con detalles de I/O.

## Clases

- Preferir composición sobre herencia.
- Inmutabilidad por defecto: campos `final`, objetos de valor como records.
- Tamaño orientativo: si no entra en pantalla sin scroll, probablemente hace demasiado.

## Comentarios

- El código bien nombrado no necesita comentarios que expliquen el qué.
- Los comentarios válidos explican el porqué: una decisión no obvia, una restricción externa,
  un workaround documentado.
- Nunca dejar código comentado — para eso existe git.

## No repetirse (DRY)

- Si copias un bloque por segunda vez, extraerlo.
- La duplicación en tests es aceptable cuando mejora la legibilidad de cada test por separado.

## Fail fast

- Validar precondiciones al inicio del método y lanzar excepción inmediatamente.
- Evitar null: usar Optional<T> para retornos que pueden no tener valor.
  Nunca retornar null desde un método público.
- No capturar excepciones para ignorarlas. Si se captura, se maneja o se relanza con contexto.