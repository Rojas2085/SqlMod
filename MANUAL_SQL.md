# 📘 Manual Completo de Variables, Comodines (`%`) e Inserciones — `sqlmod`

Este manual detalla todas las **variables (columnas virtuales)**, **patrones de comodines (`LIKE '%...%'`)**, **inserciones entre cofres** y **comandos avanzados** disponibles tanto en la **Terminal SQL** como en la **Terminal SQL Portátil**.

---

## 1. Tabla Maestra de Variables (Columnas Virtuales)

Cada objeto dentro de un cofre tiene variables que puedes usar en `WHERE`, `ORDER BY` y `GROUP BY`. Puedes escribirlas tanto en **español** como en **inglés**:

| Variable (Español / Inglés) | Tipo | Operadores Soportados | Valores / Ejemplos |
| :--- | :--- | :--- | :--- |
| `nombre` / `name` / `item_id` | Texto | `=`, `LIKE` | `'diamond'`, `'%ingot%'`, `'%espada%'` *(busca en el ID interno y en el nombre traducido)* |
| `cantidad` / `count` | Número (`1`–`64`) | `=`, `!=`, `>`, `<`, `>=`, `<=` | `cantidad = 64`, `count < 16`, `cantidad >= 32` |
| `dano` / `damage` | Número (`0`+) | `=`, `!=`, `>`, `<`, `>=`, `<=` | `dano = 0` *(nuevo)*, `damage > 100` *(gastado)* |
| `durabilidad_pct` / `durability_pct` | Porcentaje (`0`–`100`) | `=`, `!=`, `>`, `<`, `>=`, `<=` | `durabilidad_pct < 15` *(a punto de romperse)*, `durabilidad_pct = 100` *(intacto)* |
| `mod` | Texto | `=`, `LIKE` | `mod = 'minecraft'`, `mod LIKE '%sqlmod%'`, `mod = 'create'` |
| `etiqueta` / `tag` | Texto | `=`, `LIKE` | `etiqueta LIKE '%ores%'`, `tag = 'logs'`, `tag LIKE '%ingots%'`, `tag = 'planks'` |
| `rareza` / `rarity` | Texto | `=` | `'common'` *(blanco)*, `'uncommon'` *(amarillo)*, `'rare'` *(cian)*, `'epic'` *(morado)* |
| `encantado` / `enchanted` | Booleano | `=` | `true`, `false`, `1`, `0`, `si` *(incluye armas, armaduras y libros encantados)* |
| `apilable` / `stackable` | Booleano | `=` | `true` *(bloques/materiales)*, `false` *(espadas, picos, pociones, armaduras)* |
| `es_comida` / `is_food` | Booleano | `=` | `true` *(cualquier alimento comestible)*, `false` |

> [!WARNING]
> **Revisa bien los nombres de las columnas.** Si escribes una columna que no existe (ej. `nombr LIKE '%ingot%'`), la condición se ignora y **coincide con TODO**. Antes de un `DELETE FROM`, prueba siempre el mismo filtro con `SELECT COUNT(*) FROM ... WHERE ...`.

> [!NOTE]
> En columnas de texto (`nombre`, `mod`, `etiqueta`), el operador `=` funciona como **"contiene"**: `nombre = 'diamond'` también captura `diamond_sword`. Para coincidencias exactas usa `LIKE 'diamond'` (sin `%`).

---

## 2. Guía de Comodines (`LIKE` y `%`)

El operador `LIKE` permite filtrar objetos por fragmentos de su nombre, etiqueta (`tag`) o `mod` usando el símbolo `%`:

| Posición del `%` | Significado | Ejemplo SQL | Qué captura |
| :--- | :--- | :--- | :--- |
| **`'%texto%'`** | Contiene la palabra en **cualquier parte** | `WHERE nombre LIKE '%ingot%'` | `iron_ingot`, `gold_ingot`, `netherite_ingot`, `copper_ingot` |
| **`'texto%'`** | **Empieza** exactamente por esa palabra | `WHERE nombre LIKE 'diamond%'` | `diamond`, `diamond_sword`, `diamond_pickaxe`, `diamond_block` |
| **`'%texto'`** | **Termina** exactamente en esa palabra | `WHERE nombre LIKE '%_ore'` | `iron_ore`, `deepslate_diamond_ore`, `coal_ore`, `gold_ore` |

### Diccionario de Comodines Más Útiles en Minecraft
* **Lingotes:** `WHERE nombre LIKE '%ingot%'` (o `WHERE etiqueta LIKE '%ingots%'`)
* **Minerales en bruto / Menas:** `WHERE nombre LIKE '%raw_%' OR nombre LIKE '%_ore'`
* **Pepitas:** `WHERE nombre LIKE '%nugget%'`
* **Espadas / Picos / Hachas:** `WHERE nombre LIKE '%sword%' OR nombre LIKE '%pickaxe%' OR nombre LIKE '%axe%'`
* **Armaduras:** `WHERE nombre LIKE '%helmet%' OR nombre LIKE '%chestplate%' OR nombre LIKE '%leggings%' OR nombre LIKE '%boots%'`
* **Troncos y Maderas:** `WHERE nombre LIKE '%log%' OR nombre LIKE '%planks%'` (o `WHERE tag = 'logs'`)
* **Semillas y Cultivos:** `WHERE nombre LIKE '%seeds%' OR nombre LIKE '%wheat%'`
* **Pociones y Libros:** `WHERE nombre LIKE '%potion%' OR nombre LIKE '%book%'`

---

## 3. Manual de Inserciones y Movimiento de Ítems

### A. Inserción desde la `SQL Output Box` hacia un Cofre (`INSERT INTO`)
```sql
INSERT INTO almacen
INSERT INTO minerales WHERE nombre LIKE '%ingot%'
INSERT INTO cocina WHERE es_comida = true
INSERT INTO armeria WHERE encantado = true
```

### B. Inserción Directa de Cofre a Cofre (`INSERT INTO ... SELECT ...`)
```sql
INSERT INTO minerales SELECT * FROM * WHERE nombre LIKE '%ingot%' OR nombre LIKE '%diamond%'
INSERT INTO reparar SELECT * FROM armeria WHERE apilable = false AND durabilidad_pct < 20
INSERT INTO construccion SELECT * FROM mina WHERE nombre LIKE '%stone%' ORDER BY cantidad DESC LIMIT 10
```

### C. Reparto Equitativo a Múltiples Cofres (`DISTRIBUTE`)
```sql
DISTRIBUTE FROM combustible INTO (horno1, horno2, horno3) WHERE nombre LIKE '%coal%'
DISTRIBUTE FROM * INTO (fundicion1, fundicion2) WHERE nombre LIKE '%_ore' OR nombre LIKE '%raw_%'
```

### D. Confirmar (`COMMIT TO`) o Revertir (`ROLLBACK`) la `Output Box`
```sql
ROLLBACK
COMMIT TO boveda
```

---

## 4. Estadísticas, Compactación y Automatización

```sql
-- Estadísticas y agregación
SELECT COUNT(*) FROM * WHERE nombre LIKE '%ingot%'
SELECT mod, SUM(cantidad) FROM * GROUP BY mod
SELECT nombre, SUM(cantidad) FROM minerales GROUP BY nombre HAVING SUM(cantidad) < 32

-- Compactar y ordenar cofres
OPTIMIZE TABLE *
ALTER TABLE * ORDER BY nombre ASC

-- Macros
CREATE MACRO clasificar_todo AS INSERT INTO minerales WHERE nombre LIKE '%ingot%'; INSERT INTO cocina WHERE es_comida = true; OPTIMIZE TABLE *
EXEC clasificar_todo
```

> [!TIP]
> Las **Macros** (`CREATE MACRO`) y **Vistas** (`CREATE VIEW`) se guardan de forma **100% persistente** en los datos del mundo (`data/sqlmod_scripts.dat`), por lo que nunca se perderán al reiniciar el servidor, salir del mundo o recargar chunks. Los **Triggers** se guardan en el bloque físico de la terminal.

---

## 5. Guía Completa de Triggers (Disparadores Automáticos)

Los **Triggers** permiten que la Terminal SQL vigile un contenedor en tiempo real y ejecute automáticamente sentencias SQL en cuanto su contenido cambie y cumpla una condición.

### Sintaxis Oficial
```sql
CREATE TRIGGER <nombre> ON <fuente> [WHERE <condicion>] DO <accion>
```

* **`<nombre>`:** Identificador único del trigger (ej. `auto_lingotes`, `vaciar_tolva`).
* **`ON <fuente>`:** El contenedor vigilado. Puede ser:
  * `OutputBox` (o `output`): Vigila la caja de salida de la terminal.
  * El nombre de cualquier cofre vinculado (ej. `buzon`, `entrada`, `mina`).
* **`WHERE <condicion>` (Opcional):** Filtro que evalúa si los ítems insertados/modificados deben disparar la acción (usa las variables `nombre`, `etiqueta`, `count`, `durabilidad_pct`, `rareza`, `encantado`, `es_comida`, etc.). Si se omite, se dispara ante cualquier cambio en el cofre.
* **`DO <accion>`:** Comando SQL a ejecutar (puedes encadenar varios comandos con `;`).

### Comandos de Gestión de Triggers
```sql
SHOW TRIGGERS                 -- Muestra todos los triggers registrados y su estado [ON/OFF]
ENABLE TRIGGER <nombre>       -- Activa el trigger
DISABLE TRIGGER <nombre>      -- Desactiva temporalmente el trigger sin borrarlo
DROP TRIGGER <nombre>         -- Elimina permanentemente el trigger
```

*(En la pantalla de la Terminal SQL, el panel derecho muestra la lista de triggers en tiempo real con indicadores luminosos verdes `[ON]` o rojos `[OFF]`).*

---

### 💡 Ejemplos Prácticos de Triggers para Automatizar tu Base

#### Ejemplo 1: "Cofre de Descarga" Inteligente (Drop & Go)
Colocas tus ítems en la `OutputBox` (o en un cofre llamado `buzon`) al llegar de minar o explorar, y la terminal los clasifica al instante:
```sql
-- Si caen lingotes o gemas en la OutputBox, muévelos al cofre 'minerales'
CREATE TRIGGER auto_minerales ON OutputBox WHERE nombre LIKE '%ingot%' OR nombre LIKE '%diamond%' DO INSERT INTO minerales WHERE nombre LIKE '%ingot%' OR nombre LIKE '%diamond%'

-- Si cae comida en la OutputBox, muévela a 'cocina'
CREATE TRIGGER auto_comida ON OutputBox WHERE es_comida = true DO INSERT INTO cocina WHERE es_comida = true
```

#### Ejemplo 2: Alimentador Automático de Hornos
Cuando el cofre `mina` reciba minerales en bruto (`raw_iron`, `raw_gold`, etc.), repártelos automáticamente entre los hornos:
```sql
CREATE TRIGGER fundir_menas ON mina WHERE nombre LIKE '%raw_%' DO DISTRIBUTE FROM mina INTO (horno1, horno2, horno3) WHERE nombre LIKE '%raw_%' LIMIT 64
```

#### Ejemplo 3: Separador de Equipamiento Dañado
Vigila un cofre de botín `drop_mobs` y cuando caigan armaduras o espadas rotas (< 25% de durabilidad), envíalas a un cofre de reparación o al Reciclador:
```sql
CREATE TRIGGER separar_rotos ON drop_mobs WHERE apilable = false AND durabilidad_pct < 25 DO INSERT INTO reparar SELECT * FROM drop_mobs WHERE apilable = false AND durabilidad_pct < 25
```

#### Ejemplo 4: Compactación Automática de Almacén
Cada vez que entre nuevo material al cofre `almacen_general`, fusiona stacks incompletos y mantén todo ordenado:
```sql
CREATE TRIGGER auto_orden ON almacen_general DO OPTIMIZE TABLE almacen_general; ALTER TABLE almacen_general ORDER BY nombre ASC
```

#### Ejemplo 5: Descarte Automático de Basura hacia el Reciclador
Si en tu cofre de granja `granja_mobs` entra carne podrida o flechas en exceso, elimínalas automáticamente:
```sql
CREATE TRIGGER auto_limpieza ON granja_mobs WHERE nombre LIKE '%rotten_flesh%' DO DELETE FROM granja_mobs WHERE nombre LIKE '%rotten_flesh%'
```

---

## 6. Terminal SQL Portátil (tabla `infectados`)

```sql
SHOW ITEMS
SELECT COUNT(*) FROM infectados WHERE nombre LIKE '%diamond%'
SELECT mod, SUM(cantidad) FROM infectados GROUP BY mod
SELECT * FROM infectados WHERE rareza = 'epic' OR encantado = true
```

> [!NOTE]
> Si la Terminal Portátil no tiene una Output Box vinculada, cualquier `SELECT` funciona como **vista previa**: muestra las coincidencias sin extraer nada.


---

## 11. Variables Booleanas Inteligentes y Lotes (`v1.1.0`)

Además de `nombre LIKE '%...%'`, ahora puedes usar **variables booleanas automáticas** en cualquier `WHERE` o `CREATE TRIGGER` (soportan paréntesis `(...)`, `AND`, `OR` y `NOT LIKE`):

| Variable (Español / Inglés) | ¿Qué detecta automáticamente? |
| :--- | :--- |
| `es_mineral = true` / `is_mineral` | Lingotes, minerales en bruto (`raw_`), gemas, diamantes, esmeraldas, pepitas y polvos (excluye armas, armaduras y herramientas). |
| `es_arma = true` / `is_weapon` | Espadas, arcos, ballestas, tridentes, mazas, dagas y armas de mods. |
| `es_armadura = true` / `is_armor` | Cascos, pecheras, grebas, botas, escudos, élitros y armaduras de caballo (`horse_armor`). |
| `es_herramienta = true` / `is_tool` | Picos, hachas, palas, azadas, tijeras, cañas de pescar y llaves inglesas. |
| `es_bloque = true` / `is_block` | Cualquier ítem que sea colocable como bloque en el mundo (`BlockItem`). |
| `es_libro = true` / `is_book` | Libros normales, libros encantados, tomos y manuales de mods. |
| `es_comida = true` / `is_food` | Cualquier alimento comestible vanilla o de mods. |
| `es_pocion = true` / `is_potion` | Pociones normales, arrojadizas, persistentes y botellas de experiencia. |
| `es_netherite = true` / `is_netherite` | Cualquier equipamiento o material de Netherita. |
| `multiplo_de = N` / `lote = N` / `batch = N` | Extrae **únicamente múltiplos exactos de $N$** (ej. `multiplo_de = 9` saca de 9 en 9 para compactar bloques y deja el sobrante). |

---

## 12. Nuevas Herramientas de Red y Diagnóstico SQL (`v1.1.0`)

| Herramienta | ID | Función Principal |
| :--- | :--- | :--- |
| **Terminal Inalámbrica de Bolsillo** | `sqlmod:wireless_terminal` | `Shift + Clic Derecho` en una Terminal Portátil para enlazarla; `Clic Derecho` en el aire para abrirla desde cualquier lugar del mundo. |
| **Repetidor de Expansión de Red** | `sqlmod:infection_relay` | `Shift + Clic Derecho` en un cofre infectado para copiar su red, y `Clic Derecho` en un cofre lejano para unir hasta 64 cofres más a la misma llave. |
| **Llavero Multirred** | `sqlmod:network_key_ring` | Almacena hasta 8 Llaves de Red. `Clic Derecho` con llave en mano secundaria para guardarla; `Shift + Clic Derecho` para cambiar de red activa. |
| **Vinculador Láser a Distancia** | `sqlmod:laser_chest_linker` | Apunta hasta a 32 bloques con rayo láser (`Clic Derecho`), nómbralo con `Shift + Clic Derecho` y regístralo en la Terminal SQL. |
| **Purificador de Infección** | `sqlmod:infection_purifier` | `Clic Derecho` sobre un cofre infectado para desinfectarlo y sacarlo de la red sin romperlo ni perder sus ítems. |
| **Visor HUD de Red** | `sqlmod:network_visor` | Escanea tu red con partículas y muestra en pantalla cuántos cofres están en chunks cargados (`Online`) o descargados (`Offline`). |
| **Inspector y Diagnóstico SQL** | `sqlmod:sql_inspector` | `Clic Derecho` en cualquier cofre para ver en el chat qué columnas `WHERE` (`es_mineral`, `es_arma`, `es_armadura`, etc.) tienen sus ítems. |
| **Tarjeta de Script SQL** | `sqlmod:query_card` | `Shift + Clic Derecho` en una terminal para grabar su última consulta; `Clic Derecho` en cualquier terminal para ejecutar el script con 1 clic. |
