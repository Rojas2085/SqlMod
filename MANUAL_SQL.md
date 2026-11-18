# 📘 MANUAL MAESTRO DE SQL Y AUTOMATIZACIÓN — `SQL Storage Mod (sqlmod)`

> **Versión:** `1.1.0` | **Minecraft:** `1.20.1 (Forge)`
> Guía enciclopédica de comandos SQL, funciones, variables virtuales, diccionario de palabras clave de Minecraft (`%...%`), concatenación lógica y creación de **Triggers desde nivel básico hasta industrial**.

---

## 📑 Índice General
1. [Conceptos Básicos: ¿Cómo ve SQL tus Cofres?](#1-conceptos-básicos-cómo-ve-sql-tus-cofres)
2. [Todas las Columnas y Variables Virtuales del Mod](#2-todas-las-columnas-y-variables-virtuales-del-mod)
3. [Diccionario Completo de Palabras Clave de Minecraft (`LIKE '%...%'`)](#3-diccionario-completo-de-palabras-clave-de-minecraft-like-)
4. [Operadores Lógicos, Matemáticos y Concatenaciones](#4-operadores-lógicos-matemáticos-y-concatenaciones)
5. [Todos los Comandos y Funciones SQL del Motor](#5-todos-los-comandos-y-funciones-sql-del-motor)
6. [Guía Definitiva de Triggers (`CREATE TRIGGER`)](#6-guía-definitiva-de-triggers-create-trigger)
7. [Ejemplos Nivel 1: Sencillos (1 sola condición)](#7-ejemplos-nivel-1-sencillos-1-sola-condición)
8. [Ejemplos Nivel 2: Intermedios (Combinaciones `AND`, `OR`, `NOT LIKE`)](#8-ejemplos-nivel-2-intermedios-combinaciones-and-or-not-like)
9. [Ejemplos Nivel 3: Avanzados e Industriales (Paréntesis, Ciclos y Scripts Maestros)](#9-ejemplos-nivel-3-avanzados-e-industriales-paréntesis-ciclos-y-scripts-maestros)
10. [Guía de las 11 Herramientas y Bloques del Mod](#10-guía-de-las-11-herramientas-y-bloques-del-mod)

---

## 1. Conceptos Básicos: ¿Cómo ve SQL tus Cofres?

En **SQL Storage Mod**, tu sistema de almacenamiento funciona exactamente igual que una base de datos relacional real:
* **Base de Datos (`Database`):** Es tu **Terminal SQL** o tu **Red de Cofres Infectados**.
* **Tabla (`Table`):** Es cada **Cofre vinculado** con un nombre (por ejemplo: `Minerales`, `Armamento`, `Bloques`, `ServOut`, `OutputBox`).
  * Si usas `*` en el lugar de la tabla (`FROM *`), la consulta buscará en **todos los cofres vinculados al mismo tiempo**.
* **Fila (`Row` / Registro):** Es cada **slot ocupado** dentro de un cofre.
* **Columna (`Column`):** Es cada propiedad o atributo del ítem que está en ese slot (`nombre`, `cantidad`, `dano`, `es_mineral`, etc.).

---

## 2. Todas las Columnas y Variables Virtuales del Mod

Puedes usar cualquiera de estas variables tanto en `SELECT`, `WHERE`, `DELETE`, `UPDATE` como en `CREATE TRIGGER` (funcionan tanto en español como en inglés):

### A. Variables de Texto e Identificación
| Variable | Qué evalúa | Ejemplo de uso |
| :--- | :--- | :--- |
| `nombre` / `name` | Nombre interno de registro (`iron_ingot`) o nombre visible en pantalla (`Lingote de hierro`). | `WHERE nombre LIKE '%ingot%'` |
| `item_id` | ID completo con namespace (`minecraft:diamond`). | `WHERE item_id = 'minecraft:diamond'` |
| `mod` | Nombre del mod al que pertenece el objeto (`minecraft`, `create`, `sqlmod`, `tombstone`). | `WHERE mod = 'create'` |
| `etiqueta` / `tag` | Etiquetas oficiales de Forge del ítem (`forge:ingots`, `minecraft:logs`, `forge:ores`). | `WHERE etiqueta LIKE '%ores%'` |
| `rareza` / `rarity` | Rareza oficial del ítem: `COMMON`, `UNCOMMON`, `RARE`, `EPIC`. | `WHERE rareza = 'EPIC'` |
| `tabla` / `cofre` | Nombre del cofre donde está guardado el ítem. | `SELECT tabla, SUM(cantidad) FROM * GROUP BY tabla` |

### B. Variables Numéricas y de Lotes
| Variable | Qué evalúa | Ejemplo de uso |
| :--- | :--- | :--- |
| `cantidad` / `count` | Cantidad de ítems apilados en ese slot (`1` a `64`, o más en cofres mejorados). | `WHERE cantidad >= 32` |
| `dano` / `damage` | Puntos de durabilidad gastados (`0` = nuevo/intacto, `> 0` = usado/dañado). | `WHERE dano > 0` |
| `durabilidad_pct` / `durability_pct` | Porcentaje de vida útil restante del ítem (`0` a `100`). | `WHERE durabilidad_pct < 50` |
| `slot` | Número del slot dentro del cofre (`0`, `1`, `2`...). | `WHERE slot < 9` |
| `multiplo_de` / `lote` / `batch` | **¡Especial para fábricas!** Solo extrae múltiplos exactos de $N$ y deja el resto en el cofre. Ignora stacks menores a $N$. | `WHERE multiplo_de = 9` *(saca 9, 18, 27, 36, 45, 54 o 63)* |

### C. Variables Booleanas Inteligentes (`true` / `false`)
Detectan automáticamente la categoría del objeto en Minecraft (vanilla y de mods) sin que tengas que escribir decenas de `LIKE`:

| Variable Booleana | ¿Cuándo es `true`? | Ejemplo |
| :--- | :--- | :--- |
| `es_mineral` / `is_mineral` | Lingotes (`ingot`), minerales crudos (`raw_`), menas (`ore`), gemas (`gem`), diamantes, esmeraldas, pepitas (`nugget`), redstone, lapislázuli, cuarzo, amatista y polvos (`dust`). **Excluye automáticamente armas, armaduras y herramientas.** | `WHERE es_mineral = true` |
| `es_arma` / `is_weapon` | Espadas (`SwordItem`), arcos (`BowItem`), ballestas (`CrossbowItem`), tridentes (`TridentItem`), mazas, dagas, lanzas y katanas de mods. | `WHERE es_arma = true` |
| `es_armadura` / `is_armor` | Cascos (`helmet`), pecheras (`chestplate`), grebas (`leggings`), botas (`boots`), escudos (`ShieldItem`), élitros (`ElytraItem`) y armaduras de caballo (`horse_armor`). | `WHERE es_armadura = true` |
| `es_herramienta` / `is_tool` | Picos (`pickaxe`), hachas (`axe`), palas (`shovel`), azadas (`hoe`), tijeras (`shears`), cañas de pescar (`fishing_rod`), mecheros y llaves (`wrench`). | `WHERE es_herramienta = true` |
| `es_bloque` / `is_block` | Cualquier objeto que se pueda colocar como bloque en el mundo (`BlockItem`), como piedra, madera, tierra, bloques de mineral, etc. | `WHERE es_bloque = true` |
| `es_libro` / `is_book` | Libros normales, libros encantados (`enchanted_book`), libros escritos, tomos y guías de mods. | `WHERE es_libro = true` |
| `es_comida` / `is_food` | Cualquier ítem que el jugador pueda comer (`Item.isEdible()`). | `WHERE es_comida = true` |
| `es_pocion` / `is_potion` | Pociones normales, arrojadizas (`splash`), persistentes (`lingering`) y botellas de experiencia. | `WHERE es_pocion = true` |
| `es_netherite` / `is_netherite` | Lingotes, fragmentos, armas, herramientas o armaduras de Netherita. | `WHERE es_netherite = false` |
| `encantado` / `enchanted` | `true` si el ítem tiene encantamientos activos o si es un libro encantado (`StoredEnchantments`). | `WHERE encantado = true` |
| `apilable` / `stackable` | `true` si el ítem se puede apilar en grupos de más de 1; `false` para espadas, armaduras, pociones, etc. | `WHERE apilable = false` |

---

## 3. Diccionario Completo de Palabras Clave de Minecraft (`LIKE '%...%'`)

En Minecraft, los IDs internos están en inglés. Cuando usas `nombre LIKE '%palabra%'`, los signos de porcentaje `%` significan **"cualquier texto antes o después"**:
* `'iron_ingot'` = coincide exactamente con `iron_ingot`.
* `'iron_%'` = empieza con `iron_` (ej. `iron_ingot`, `iron_sword`, `iron_helmet`).
* `'%_ingot'` = termina en `_ingot` (ej. `iron_ingot`, `gold_ingot`, `copper_ingot`).
* `'%ingot%'` = contiene la palabra `ingot` en cualquier parte.

Aquí tienes las palabras clave más útiles de Minecraft divididas por categoría:

### 💎 3.1. Minerales, Lingotes y Forja
| Patrón `LIKE` | Qué objetos detecta en Minecraft |
| :--- | :--- |
| `'%ingot%'` | Todos los lingotes (`iron_ingot`, `gold_ingot`, `copper_ingot`, `netherite_ingot`, `brass_ingot`, `zinc_ingot`). |
| `'%nugget%'` | Todas las pepitas (`iron_nugget`, `gold_nugget`, `copper_nugget`, `zinc_nugget`). |
| `'%raw_%'` | Minerales en bruto sin fundir (`raw_iron`, `raw_gold`, `raw_copper`, `raw_zinc`). |
| `'%ore%'` | Bloques de mena picados con Toque de Seda (`iron_ore`, `deepslate_diamond_ore`, `gold_ore`). |
| `'%diamond%'` | Diamantes y derivados (`diamond`, `diamond_block`, `diamond_sword` -> *Combínalo con `AND es_mineral = true` o `AND nombre NOT LIKE '%block%'` si solo quieres la gema*). |
| `'%emerald%'` | Esmeraldas (`emerald`, `emerald_block`). |
| `'%amethyst%'` | Fragmentos y bloques de amatista (`amethyst_shard`, `amethyst_block`, `amethyst_cluster`). |
| `'%quartz%'` | Cuarzo del Nether (`quartz`, `quartz_block`). |
| `'%lapis%'` | Lapislázuli (`lapis_lazuli`, `lapis_block`). |
| `'%redstone%'` | Polvo, bloques y componentes de Redstone (`redstone`, `redstone_block`, `redstone_torch`). |
| `'%coal%'` | Carbón y carbón vegetal (`coal`, `charcoal`, `coal_block`). |
| `'%dust%'` | Polvos como `glowstone_dust`, `redstone`, polvos de mods. |
| `'%scrap%'` | Fragmentos de netherita (`netherite_scrap`). |

### ⚔️ 3.2. Armas, Armaduras y Herramientas
| Patrón `LIKE` | Qué objetos detecta en Minecraft |
| :--- | :--- |
| `'%sword%'` | Todas las espadas (`wooden_sword`, `stone_sword`, `iron_sword`, `diamond_sword`, `netherite_sword`). |
| `'%bow%'` | Arcos y ballestas (`bow`, `crossbow`). |
| `'%arrow%'` | Todo tipo de flechas (`arrow`, `spectral_arrow`, `tipped_arrow`). |
| `'%trident%'` | Tridentes (`trident`). |
| `'%shield%'` | Escudos (`shield`). |
| `'%helmet%'` | Todos los cascos (`leather_helmet`, `iron_helmet`, `diamond_helmet`, `turtle_helmet`). |
| `'%chestplate%'` | Todas las pecheras (`leather_chestplate`, `iron_chestplate`, `diamond_chestplate`). |
| `'%leggings%'` | Todos los pantalones/grebas (`leather_leggings`, `iron_leggings`, `diamond_leggings`). |
| `'%boots%'` | Todas las botas (`leather_boots`, `iron_boots`, `diamond_boots`, `golden_boots`). |
| `'%horse_armor%'` | Armaduras de caballo (`iron_horse_armor`, `golden_horse_armor`, `diamond_horse_armor`). |
| `'%barding%'` | Bardas/armaduras de caballo de mods como *Epic Knights / Magistu Armory*. |
| `'%pickaxe%'` | Todos los picos (`iron_pickaxe`, `diamond_pickaxe`, etc.). |
| `'%_axe%'` | Todas las hachas (al poner el guion bajo `'_axe%'` evitas que se confunda con `pickaxe`). |
| `'%shovel%'` | Todas las palas (`iron_shovel`, `diamond_shovel`). |
| `'%hoe%'` | Todas las azadas (`iron_hoe`, `diamond_hoe`). |
| `'%shears%'` | Tijeras / cizallas (`shears`). |

### 🧱 3.3. Bloques de Construcción y Naturaleza
| Patrón `LIKE` | Qué objetos detecta en Minecraft |
| :--- | :--- |
| `'%block%'` | Bloques compactos (`iron_block`, `gold_block`, `diamond_block`, `grass_block`, `bone_block`, `hay_block`). |
| `'%stone%'` | Piedra, adoquín, piedra lisa, arenisca (`stone`, `cobblestone`, `smooth_stone`, `sandstone`, `blackstone`, `end_stone`). |
| `'%deepslate%'` | Pizarra profunda y sus variantes (`deepslate`, `cobbled_deepslate`, `deepslate_bricks`). |
| `'%log%'` | Troncos de madera (`oak_log`, `spruce_log`, `birch_log`, `dark_oak_log`, `cherry_log`). |
| `'%planks%'` | Tablones de madera (`oak_planks`, `spruce_planks`, `crimson_planks`). |
| `'%sapling%'` | Brotes de árboles (`oak_sapling`, `spruce_sapling`, `birch_sapling`, `cherry_sapling`). |
| `'%leaves%'` | Hojas de árboles (`oak_leaves`, `spruce_leaves`). |
| `'%brick%'` | Ladrillos (`bricks`, `stone_bricks`, `nether_bricks`, `mud_bricks`). |
| `'%glass%'` | Cristal y paneles de cristal (`glass`, `glass_pane`, `white_stained_glass`). |
| `'%wool%'` | Lana de cualquier color (`white_wool`, `red_wool`, `black_wool`). |
| `'%concrete%'` | Hormigón y polvo de hormigón (`white_concrete`, `black_concrete_powder`). |
| `'%slab%'` / `'%stairs%'` | Todas las losas (`_slab`) y escaleras (`_stairs`). |
| `'%dirt%'` / `'%sand%'` / `'%gravel%'` | Tierra (`dirt`, `coarse_dirt`), arena (`sand`, `red_sand`) y grava (`gravel`). |

### 🍖 3.4. Comida, Cultivos y Pociones
| Patrón `LIKE` | Qué objetos detecta en Minecraft |
| :--- | :--- |
| `'%cooked_%'` | Toda carne o pescado ya cocinado (`cooked_beef`, `cooked_porkchop`, `cooked_chicken`, `cooked_mutton`, `cooked_cod`, `cooked_salmon`). |
| `'%beef%'` / `'%porkchop%'` / `'%chicken%'` | Carne de res, cerdo o pollo (cruda o cocinada). |
| `'%bread%'` / `'%apple%'` / `'%carrot%'` / `'%potato%'` | Pan, manzanas (`apple`, `golden_apple`), zanahorias (`carrot`, `golden_carrot`), papas (`potato`, `baked_potato`). |
| `'%seed%'` | Todas las semillas (`wheat_seeds`, `pumpkin_seeds`, `melon_seeds`, `beetroot_seeds`, `torchflower_seeds`). |
| `'%potion%'` | Todas las pociones (`potion`, `splash_potion`, `lingering_potion`). |

### 💀 3.5. Drops de Granjas de Mobs
| Patrón `LIKE` | Qué objetos detecta en Minecraft |
| :--- | :--- |
| `'bone'` | Únicamente el hueso normal (`minecraft:bone`), sin incluir polvo ni bloques de hueso. |
| `'%bone%'` | Hueso (`bone`), polvo de hueso (`bone_meal`) y bloque de hueso (`bone_block`). |
| `'%rotten_flesh%'` | Carne podrida de zombi. |
| `'%gunpowder%'` | Pólvora de creeper. |
| `'%string%'` | Hilo de araña. |
| `'%spider_eye%'` | Ojo de araña (`spider_eye`, `fermented_spider_eye`). |
| `'%ender_pearl%'` | Perlas de Ender. |
| `'%blaze%'` | Varas y polvo de Blaze (`blaze_rod`, `blaze_powder`). |
| `'%slime%'` | Bolas y bloques de Slime (`slime_ball`, `slime_block`). |
| `'%leather%'` | Cuero (`leather`). |
| `'%feather%'` | Plumas (`feather`). |

---

## 4. Operadores Lógicos, Matemáticos y Concatenaciones

El motor SQL de `sqlmod` soporta expresiones booleanas completas y anidadas:

### 4.1. Operadores de Comparación
* `=` : Igual a (`cantidad = 64`, `es_mineral = true`, `rareza = 'EPIC'`).
* `!=` : Diferente de (`dano != 0`).
* `>` : Mayor que (`cantidad > 16`, `dano > 0`).
* `<` : Menor que (`durabilidad_pct < 50`).
* `>=` : Mayor o igual que (`cantidad >= 9`).
* `<=` : Menor o igual que (`dano <= 10`).
* `LIKE` : Coincidencia por patrón de texto (`nombre LIKE '%ingot%'`).
* `NOT LIKE` : **Exclusión por patrón de texto** (`nombre NOT LIKE '%block%'`).

### 4.2. Concatenación Lógica (`AND`, `OR`, `NOT` y Paréntesis `(...)`)
1. **`AND` (Y lógico):** **Ambas** condiciones deben cumplirse al mismo tiempo.
   * Ejemplo: `es_mineral = true AND es_bloque = false` *(tiene que ser un mineral Y NO ser un bloque)*.
2. **`OR` (O lógico):** Basta con que **cualquiera** de las condiciones se cumpla.
   * Ejemplo: `es_comida = true OR es_pocion = true` *(entra si es comida O si es poción)*.
3. **`NOT` (Negación):** Invierte el resultado de una condición o grupo.
   * Ejemplo: `NOT (es_arma = true OR es_armadura = true)`
4. **Paréntesis `(...)` (Agrupación de prioridad):**
   * En SQL, el operador `AND` tiene mayor prioridad que el `OR`. Por eso, cuando mezclas varios `OR` con un `AND`, **debes encerrar los `OR` entre paréntesis**:
   * ✅ **Correcto:** `(nombre LIKE '%ingot%' OR nombre LIKE '%diamond%') AND nombre NOT LIKE '%block%'`
   * ❌ **Sin paréntesis:** `nombre LIKE '%ingot%' OR (nombre LIKE '%diamond%' AND nombre NOT LIKE '%block%')` *(dejaría pasar `iron_block` porque contiene `iron` o cumpliría el primer lado del `OR`)*.

---

## 5. Todos los Comandos y Funciones SQL del Motor

### 5.1. `SELECT` (Consultar y Extraer a la Output Box)
```sql
SELECT <columnas> FROM <cofre | *> [WHERE <condiciones>] [ORDER BY <columna> ASC|DESC] [LIMIT <N>]
```
* Si tienes una **Caja de Salida (`OutputBox`)** vinculada, los ítems que coincidan se extraerán físicamente hacia la `OutputBox`.
* Si quieres ordenar qué sale primero: `ORDER BY cantidad DESC`, `ORDER BY dano ASC`, `ORDER BY rareza DESC`, `ORDER BY nombre ASC`.
* Si quieres limitar cuántos slots saca: `LIMIT 5`.

### 5.2. Funciones de Agregación (`COUNT`, `SUM`, `AVG`, `MIN`, `MAX` + `GROUP BY`)
Las consultas con funciones de agregación **NO extraen ítems**; solo calculan estadísticas y las muestran en pantalla:
```sql
-- Ver el total de ítems y slots ocupados en toda la red
SELECT COUNT(*), SUM(cantidad) FROM *

-- Ver cuántos ítems hay en cada cofre por separado
SELECT tabla, COUNT(*), SUM(cantidad) FROM * GROUP BY tabla

-- Ver el promedio de daño y el daño máximo en el cofre de Armamento
SELECT AVG(dano), MAX(dano), MIN(dano) FROM Armamento WHERE dano > 0
```

### 5.3. `INSERT INTO` (Transferir Ítems de un Cofre a Otro)
Tiene dos modos de uso:
1. **Modo Rápido (desde `OutputBox`):**
   ```sql
   INSERT INTO Minerales
   ```
   *(Envía todo lo que esté en la `OutputBox` hacia el cofre `Minerales`).*
2. **Modo Directo entre Cofres (`INSERT INTO ... SELECT`):**
   ```sql
   INSERT INTO <CofreDestino> SELECT * FROM <CofreOrigen | *> WHERE <condiciones> [LIMIT <N>]
   ```
   *(Mueve directamente los ítems que cumplan el `WHERE` desde el cofre origen hasta el cofre destino sin pasar por la `OutputBox`).*

### 5.4. `DISTRIBUTE FROM` (Reparto Equitativo / Load Balancing)
Divide y reparte los ítems de un cofre origen en partes iguales entre varios cofres destino (ideal para alimentar hornos en paralelo):
```sql
DISTRIBUTE FROM ServOut INTO Horno1, Horno2, Horno3 WHERE nombre LIKE '%raw_%'
```

### 5.5. `OPTIMIZE TABLE` y `ALTER TABLE ... ORDER BY` (Mantenimiento Físico)
```sql
-- Junta todos los stacks sueltos incompletos y compacta los huecos vacíos en todos los cofres
OPTIMIZE TABLE *

-- Ordena físicamente los ítems dentro de un cofre
ALTER TABLE Minerales ORDER BY cantidad DESC
SORT TABLE Armamento BY rareza DESC
```

### 5.6. Vistas Virtuales (`CREATE VIEW`)
Guarda una consulta frecuente bajo un nombre corto para usarla como si fuera una tabla:
```sql
CREATE VIEW ArmasRotas AS SELECT * FROM * WHERE (es_arma = true OR es_armadura = true) AND dano > 0
SELECT * FROM ArmasRotas
SHOW VIEWS
DROP VIEW ArmasRotas
```

### 5.7. Cruce de Cofres (`JOIN`)
Encuentra qué ítems están repetidos en dos cofres distintos al mismo tiempo:
```sql
SELECT * FROM Minerales JOIN ServOut
```

### 5.8. Transacciones (`COMMIT TO` y `ROLLBACK`)
```sql
-- Devuelve todos los ítems de la OutputBox exactamente al cofre y slot de donde salieron
ROLLBACK

-- Confirma y envía todo el contenido de la OutputBox a un cofre destino
COMMIT TO Bloques
```

---

## 6. Guía Definitiva de Triggers (`CREATE TRIGGER`)

Un **Trigger (Disparador)** es una regla automática guardada dentro de la memoria de tu **Terminal SQL**. Cada vez que el inventario del cofre vigilado cambia (por tolvas, conductos de *Create*, jugadores u otros triggers), el motor evalúa la condición y ejecuta la transferencia al instante.

### 6.1. Anatomía de un Trigger
```sql
CREATE TRIGGER <NombreDelTrigger> ON <CofreOrigen> WHERE <Condiciones> DO INSERT INTO <CofreDestino>
```
* **`<NombreDelTrigger>`:** Identificador único sin espacios (ej. `ServMinerales`, `EntEmpMine`, `GranjArmaDes`). Si creas un trigger con un nombre que ya existe, **se sobrescribe y actualiza automáticamente**.
* **`ON <CofreOrigen>`:** El cofre que el trigger vigilará (ej. `ServOut`, `OutputBox`, `Minerales`, `Granja`).
* **`WHERE <Condiciones>`:** El filtro que deben cumplir los ítems para ser movidos.
* **`DO INSERT INTO <CofreDestino>`:** El cofre hacia donde se enviarán los ítems que cumplan el filtro. *(El motor expande automáticamente `DO INSERT INTO Destino` para que extraiga únicamente desde el `<CofreOrigen>` los ítems que cumplieron el `WHERE`).*

### 6.2. Concatenar Múltiples Triggers en un Solo Script (`;`)
Puedes pegar y ejecutar **5, 10 o más triggers de un solo golpe** separándolos con punto y coma (`;`):
```sql
CREATE TRIGGER Regla1 ON CofreA WHERE ... DO INSERT INTO CofreB;
CREATE TRIGGER Regla2 ON CofreA WHERE ... DO INSERT INTO CofreC;
CREATE TRIGGER Regla3 ON CofreB WHERE ... DO INSERT INTO CofreD
```

### 6.3. Comandos de Control de Triggers
```sql
SHOW TRIGGERS                  -- Muestra la lista numerada de todos los triggers activos en la terminal
DROP TRIGGER <NombreTrigger>   -- Borra permanentemente ese trigger de la terminal
```

---

## 7. Ejemplos Nivel 1: Sencillos (1 sola condición)

Ideales para empezar o para cofres dedicados a un único tipo de material:

```sql
-- 1. Mover todos los minerales que caigan en ServOut hacia el cofre Minerales
CREATE TRIGGER MoverMinerales ON ServOut WHERE es_mineral = true DO INSERT INTO Minerales

-- 2. Mover todos los libros (normales y encantados) hacia el cofre Libros
CREATE TRIGGER MoverLibros ON ServOut WHERE es_libro = true DO INSERT INTO Libros

-- 3. Mover cualquier objeto encantado de la granja a un cofre de Tesoros
CREATE TRIGGER GuardarEncantados ON Granja WHERE encantado = true DO INSERT INTO Tesoros

-- 4. Mover únicamente huesos puros (sin polvo de hueso) al cofre de Huesos
CREATE TRIGGER SoloHuesos ON Granja WHERE nombre = 'bone' DO INSERT INTO Huesos

-- 5. Mover todos los ítems del mod Create a su propio cofre
CREATE TRIGGER TodoCreate ON ServOut WHERE mod = 'create' DO INSERT INTO CofreCreate
```

---

## 8. Ejemplos Nivel 2: Intermedios (Combinaciones `AND`, `OR`, `NOT LIKE`)

Combinan 2 o más condiciones para filtrar con precisión quirúrgica:

```sql
-- 1. Comida y Pociones juntos en un mismo cofre
CREATE TRIGGER ServComida ON ServOut WHERE es_comida = true OR es_pocion = true DO INSERT INTO ComidaYPociones

-- 2. Todo el equipamiento de combate y trabajo (armas, armaduras, herramientas, flechas y armaduras de caballo)
CREATE TRIGGER ServArmas ON ServOut WHERE es_arma = true OR es_armadura = true OR es_herramienta = true OR nombre LIKE '%arrow%' OR nombre LIKE '%horse_armor%' DO INSERT INTO Armamento

-- 3. Bloques de construcción y brotes de árboles, pero EXCLUYENDO bloques de mineral (como bloques de diamante/hierro)
CREATE TRIGGER ServBloques ON ServOut WHERE (es_bloque = true OR nombre LIKE '%sapling%') AND es_mineral = false DO INSERT INTO Bloques

-- 4. Enviar al Desmantelador/Reciclador solo armaduras y armas que estén DAÑADAS y que NO sean de Netherita
CREATE TRIGGER ReciclarDanadas ON Armamento WHERE (es_arma = true OR es_armadura = true) AND dano > 0 AND es_netherite = false DO INSERT INTO EntDes

-- 5. Filtrar lingotes y diamantes sueltos excluyendo cualquier bloque compacto
CREATE TRIGGER LingotesSueltos ON ServOut WHERE (nombre LIKE '%ingot%' OR nombre LIKE '%diamond%') AND nombre NOT LIKE '%block%' DO INSERT INTO Minerales
```

---

## 9. Ejemplos Nivel 3: Avanzados e Industriales (Paréntesis, Ciclos y Scripts Maestros)

### 🏭 9.1. Script Maestro Clasificador Central (De `ServOut` o `OutputBox` a 5 Categorías)
Copia y pega todo este bloque de una sola vez en tu terminal para automatizar la recepción de tu base:

```sql
CREATE TRIGGER ServMinerales ON ServOut WHERE es_mineral = true DO INSERT INTO Minerales;
CREATE TRIGGER ServLibros ON ServOut WHERE es_libro = true DO INSERT INTO Libros;
CREATE TRIGGER ServComida ON ServOut WHERE es_comida = true OR es_pocion = true DO INSERT INTO ComidaYPociones;
CREATE TRIGGER ServArmas ON ServOut WHERE es_arma = true OR es_armadura = true OR es_herramienta = true OR nombre LIKE '%arrow%' OR nombre LIKE '%horse_armor%' DO INSERT INTO Armamento;
CREATE TRIGGER ServBloques ON ServOut WHERE (es_bloque = true OR nombre LIKE '%sapling%') AND es_mineral = false DO INSERT INTO Bloques
```

---

### 🔄 9.2. Ciclo Industrial de Compactación de Minerales en Lotes de 9 (`multiplo_de = 9`)
**Problema clásico en Minecraft:** Quieres enviar lingotes, pepitas o diamantes desde tu cofre `Minerales` hacia una empaquetadora/prensa (`EntEmp`) para convertirlos en bloques, pero si envías cantidades impares (ej. 4 lingotes de hierro y 3 de oro), la prensa se atasca.
**Solución SQL:** Usar `multiplo_de = 9` y `es_bloque = false`. El trigger **solo sacará múltiplos exactos de 9** (9, 18, 27, 36, 45, 54, 63) y cuando el bloque compactado salga por `SalEmp`, otro trigger lo devolverá a `Minerales` (donde no volverá a salir porque ya tiene `es_bloque = true` / `%block%`):

```sql
CREATE TRIGGER EntEmpMine ON Minerales WHERE (nombre LIKE '%ingot%' OR nombre LIKE '%nugget%' OR nombre LIKE '%diamond%' OR nombre LIKE '%gem%') AND nombre NOT LIKE '%block%' AND es_bloque = false AND multiplo_de = 9 DO INSERT INTO EntEmp;
CREATE TRIGGER SalEmpMine ON SalEmp WHERE nombre LIKE '%block%' OR es_mineral = true DO INSERT INTO Minerales
```

---

### 🧟 9.3. Ciclo de Granja de Mobs + Reciclador / Desmantelador (`EntDes` $\rightarrow$ `SalDes`)
**Objetivo:** Todo el equipo basura que sueltan los mobs en `Granja` (espadas, arcos, cascos, pecheras, botas, palas) y los huesos puros van a la entrada del desmantelador (`EntDes`). Una vez desmantelados en cuero, hierro, hilos o polvo, el cofre de salida (`SalDes`) los reparte automáticamente a `Minerales`:

```sql
CREATE TRIGGER GranjArmaDes ON Granja WHERE (es_arma = true OR es_armadura = true OR es_herramienta = true OR nombre = 'bone') AND es_netherite = false DO INSERT INTO EntDes;
CREATE TRIGGER SalDesMin ON SalDes WHERE es_mineral = true DO INSERT INTO Minerales
```

> ⚠️ **Regla de Oro para evitar ciclos infinitos:**
> Si tienes un trigger que envía armaduras de `ServOut` $\rightarrow$ `Armamento`, **no pongas otro trigger en `Armamento` que envíe todas las armaduras sin filtro hacia el reciclador (`EntDes`)**, o de lo contrario cualquier armadura que guardes en `Armamento` será enviada inmediatamente a triturar. Si quieres reciclar desde `Armamento`, agrégale siempre una condición como `AND dano > 50` o hazlo directamente desde el cofre de la `Granja`.

---

## 10. Guía de las 11 Herramientas y Bloques del Mod

### 🖥️ Bloques
1. **Terminal SQL (`sql_terminal`):** Ejecuta consultas SQL, gestiona cofres con alias y procesa todos los `CREATE TRIGGER` en tiempo real.
2. **Caja de Salida SQL (`sql_output_box`):** Recibe las extracciones de `SELECT` y permite devolverlas con `ROLLBACK` o el botón *Return*.
3. **Terminal SQL Portátil (`portable_terminal`):** Se enlaza con una **Llave de Red** para consultar redes enteras de cofres infectados. Guarda sus datos al romperse con el pico.
4. **Reciclador Transmutador (`reciclador`):** Convierte ítems sobrantes en Puntos SQL para canjearlos por hierro, oro, diamantes, esmeraldas o netherite.

### 🛠️ Herramientas
1. **🔗 Vinculador de Cofres (`chest_linker`):**
   * `Shift + Clic Derecho` en un cofre: Abre ventana para asignarle nombre de tabla.
   * `Clic Derecho` en la Terminal SQL: Registra el cofre o la `OutputBox`.
2. **🎯 Vinculador Láser a Distancia (`laser_chest_linker`):**
   * Apunta desde **hasta 32 bloques** y haz `Clic Derecho` para fijar un cofre con un rayo láser. `Shift + Clic Derecho` para nombrarlo y `Clic Derecho` en la Terminal para vincularlo.
3. **🦠 Catalizador de Infección (`infection_catalyst`):**
   * `Clic Derecho` en un cofre para infectar hasta **128 cofres en un radio de 52 bloques** (con salto de 12 bloques entre cofres) y recibir su **Llave de Red**.
4. **📡 Repetidor de Expansión de Red (`infection_relay`):**
   * `Shift + Clic Derecho` en un cofre ya infectado para copiar su red, y `Clic Derecho` en otro cofre lejano para propagar la misma red sin crear otra llave.
5. **🧪 Purificador de Infección (`infection_purifier`):**
   * `Clic Derecho` en un cofre infectado para desinfectarlo y sacarlo de la red sin romperlo ni perder sus objetos.
6. **🔑 Llave de Red (`network_key`):**
   * Guarda el `UUID` de una red infectada. Haz `Clic Derecho` en una Terminal Portátil para conectarla (`Shift + Clic Derecho` con mano vacía en la terminal para retirar la llave).
7. **🗝️ Llavero Multirred (`network_key_ring`):**
   * Guarda hasta **8 Llaves de Red**. Pon una llave en tu mano secundaria y haz `Clic Derecho` para meterla al llavero. `Shift + Clic Derecho` en el aire para cambiar de red activa.
8. **📱 Terminal Inalámbrica de Bolsillo (`wireless_terminal`):**
   * `Shift + Clic Derecho` sobre una Terminal Portátil para enlazarla. Luego haz `Clic Derecho` en el aire desde cualquier lugar para abrir la pantalla de la terminal sin poner bloques.
9. **👓 Visor HUD de Red (`network_visor`):**
   * Sosténlo o haz `Clic Derecho` para lanzar un pulso de sonar que ilumina los cofres de tu red con partículas e informa cuántos están en chunks `Online` u `Offline`.
10. **🔍 Inspector y Diagnóstico SQL (`sql_inspector`):**
    * `Clic Derecho` sobre cualquier cofre para ver en el chat un análisis completo de sus columnas SQL (`es_mineral`, `es_arma`, `es_armadura`, `es_herramienta`, `es_comida`, etc.).
11. **💾 Tarjeta de Script SQL (`query_card`):**
    * `Shift + Clic Derecho` en una terminal para grabar en el chip la última consulta ejecutada; `Clic Derecho` en cualquier terminal para ejecutar el script grabado con un solo clic.
