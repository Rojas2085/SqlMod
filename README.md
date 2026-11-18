# 🗄️ SQL Storage Mod (`sqlmod`) — Real SQL Inventory Automation for Minecraft

![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen?style=for-the-badge&logo=minecraft)
![Forge 47.4.20](https://img.shields.io/badge/Forge-47.4.20-orange?style=for-the-badge)
![Java 17](https://img.shields.io/badge/Java-17-blue?style=for-the-badge&logo=openjdk)
![SQL Engine](https://img.shields.io/badge/Engine-Real_SQL_&_Triggers-00DCE6?style=for-the-badge)

> **🇬🇧 English** | **🇪🇸 Español** (Scroll down for the complete Spanish guide / Desplázate hacia abajo para la guía completa en Español)

---

## 🇬🇧 ENGLISH DOCUMENTATION

### 🚀 What is SQL Storage Mod?

**SQL Storage Mod (`sqlmod`)** transforms your Minecraft chests, barrels, and modded containers into a **real relational database**. Instead of spending hours building laggy hopper chains or complex pipe networks, you can **query, filter, sort, transfer, compact, and automate your entire storage network using real SQL statements and reactive Triggers!**

Whether you are a **Minecraft automation fan** (using mods like *Create*, *Sophisticated Storage*, *Lootr*, or *Iron Chests*) or a **student/developer looking to apply and practice real SQL skills inside a game**, `sqlmod` bridges database engineering and gameplay seamlessly.

---

### ✨ Key Features

* 💻 **Full In-Game SQL Terminal (CRT Interface):** Features syntax autocomplete (`Tab`), command history, mouse-wheel scrolling, and real-time execution logs.
* ⚡ **Reactive SQL Triggers (`CREATE TRIGGER`):** Automate item routing automatically whenever a chest's inventory changes or on Redstone pulses—zero hoppers required!
* 🌐 **Global Infected Chest Networks:** Infect up to **128 chests in a 52-block radius** with a single click and expand infinitely using **Infection Relays**.
* 📱 **Wireless Pocket Terminal:** Access your entire storage database from anywhere in the world—while mining, exploring dungeons, or building!
* 🧮 **Smart Batching & Modded Factory Integration:** Built-in modulo arithmetic (`multiplo_de = 9` / `batch = 9`) to feed exact multiples of ingots/gems into *Create* presses or auto-compactors without clogging machines!
* 🔒 **100% Atomic Transactions (`COMMIT` & `ROLLBACK`):** Zero item duplication and zero item loss. Includes a `VirtualDeliverySystem` that safely queues transfers if destination chunks are unloaded.

---

### 🧱 Blocks & Peripherals

| Block | ID | Description |
| :--- | :--- | :--- |
| **SQL Terminal** | `sqlmod:sql_terminal` | Main database server block. Links named chests (tables), executes SQL queries, and runs background `CREATE TRIGGER` automation rules. |
| **SQL Output Box** | `sqlmod:sql_output_box` | Staging buffer for `SELECT` queries. Extracted items retain origin metadata so you can return them anytime with `ROLLBACK` or the Return button! |
| **Portable SQL Terminal** | `sqlmod:portable_terminal` | Field terminal powered by a **Network Key**. Queries an entire infected chest network at once and retains its NBT state when broken. |
| **Transmuter Recycler** | `sqlmod:reciclador` | Converts surplus items into **SQL Points** to synthesize valuable resources (Iron, Gold, Diamonds, Emerald, Netherite). |

---

### 🛠️ Tools & Network Gadgets (v1.1.0 Expansion!)

1. 📱 **Wireless Pocket Terminal (`wireless_terminal`):**
   * `Shift + Right-Click` on a Portable Terminal to pair.
   * `Right-Click` in the air anywhere to open your SQL Terminal UI remotely without placing a block!
2. 🎯 **Long-Range Laser Linker (`laser_chest_linker`):**
   * `Right-Click` from up to **32 blocks away** to fire a laser beam and lock onto ceiling or hard-to-reach chests.
   * `Shift + Right-Click` to name the table, then click your SQL Terminal to link it.
3. 🦠 **Infection Catalyst (`infection_catalyst`):**
   * `Right-Click` a chest to infect all nearby containers (**52-block radius**, **12-block hop**, up to **128 chests**) and generate a **Network Key**.
4. 📡 **Infection Network Relay (`infection_relay`):**
   * Extends an existing infected network to new rooms without creating a new key! `Shift + Right-Click` an infected chest to copy its `NetworkId`, then `Right-Click` an uninfected chest far away.
5. 🔑 **Multi-Network Key Ring (`network_key_ring`):**
   * Stores up to **8 Network Keys** in a single item. Hold a key in your off-hand and `Right-Click` to store it; `Shift + Right-Click` in the air to cycle active networks.
6. 🧪 **Infection Purifier (`infection_purifier`):**
   * `Right-Click` any infected chest to safely disinfect and remove it from the network without breaking the chest or spilling items.
7. 👓 **Network HUD Visor (`network_visor`):**
   * Hold or `Right-Click` to scan your network: highlights nearby chests with green particles (`[ONLINE]`) or smoke (`[OFFLINE]`) and displays live chunk status in your HUD.
8. 🔍 **SQL Inventory Inspector (`sql_inspector`):**
   * **Learn SQL on the fly!** `Right-Click` any chest to print a holographic diagnostic report of its exact SQL virtual columns (`es_mineral`, `es_arma`, `es_armadura`, `es_herramienta`, `encantado`, etc.).
9. 💾 **SQL Query Script Card (`query_card`):**
   * Portable stored procedure! `Shift + Right-Click` a terminal to save its last query onto the card, and `Right-Click` any terminal to execute the script in one click.
10. 🔗 **Chest Linker (`chest_linker`):**
    * Standard close-range tool to assign table aliases (`Minerales`, `Armamento`, `Bloques`) and link `OutputBox` to terminals.

---

### 📊 Virtual Table Schema (Columns for `SELECT` & `WHERE`)

Every slot in a linked chest acts as a row in a SQL table with these virtual columns:

| Column (EN / ES) | Type | Example / Description |
| :--- | :--- | :--- |
| `nombre` / `name` / `item_id` | `TEXT` | `'iron_ingot'`, `'diamond_sword'`, `'minecraft:apple'` |
| `cantidad` / `count` | `INT` | `1` to `64` (stack size in that slot) |
| `dano` / `damage` | `INT` | Durability damage (`0` = brand new, `> 0` = damaged) |
| `durabilidad_pct` / `durability_pct` | `INT` | Remaining durability percentage (`0` to `100`) |
| `encantado` / `enchanted` | `BOOL` | `true` / `false` (includes Enchanted Books!) |
| `apilable` / `stackable` | `BOOL` | `true` / `false` |
| `rareza` / `rarity` | `TEXT` | `COMMON`, `UNCOMMON`, `RARE`, `EPIC` |
| `mod` | `TEXT` | Namespace of the mod (`'minecraft'`, `'create'`, `'sqlmod'`) |
| `etiqueta` / `tag` | `TEXT` | Forge item tags (`'ores'`, `'ingots'`, `'logs'`) |
| `es_mineral` / `is_mineral` | `BOOL` | `true` for ingots, raw ores, gems, diamonds, nuggets, dusts (excludes tools/armor) |
| `es_arma` / `is_weapon` | `BOOL` | `true` for swords, bows, crossbows, tridents, modded weapons |
| `es_armadura` / `is_armor` | `BOOL` | `true` for helmets, chestplates, leggings, boots, shields, elytra, horse armor |
| `es_herramienta` / `is_tool` | `BOOL` | `true` for pickaxes, axes, shovels, hoes, shears, fishing rods |
| `es_bloque` / `is_block` | `BOOL` | `true` if the item can be placed as a block (`BlockItem`) |
| `es_comida` / `is_food` | `BOOL` | `true` for any edible food item |
| `es_pocion` / `is_potion` | `BOOL` | `true` for potions and experience bottles |
| `es_libro` / `is_book` | `BOOL` | `true` for normal books, enchanted books, and guide tomes |
| `es_netherite` / `is_netherite` | `BOOL` | `true` for Netherite gear/items |
| `multiplo_de` / `lote` / `batch` | `INT` | Extracts **only exact multiples of $N$** (e.g. `multiplo_de = 9` extracts 9, 18, 27... leaving remainder) |

---

---

## 🇪🇸 DOCUMENTACIÓN EN ESPAÑOL

### 🚀 ¿Qué es SQL Storage Mod (`sqlmod`)?

**SQL Storage Mod (`sqlmod`)** convierte los cofres, barriles y contenedores de tu mundo de Minecraft en una **base de datos relacional real**. En lugar de construir enormes cadenas de tolvas con lag, puedes **consultar, filtrar, ordenar, transferir, compactar y automatizar todos tus cofres en tiempo real aplicando conocimientos reales de SQL y Triggers reactivos**.

Es el mod definitivo tanto para **jugadores técnicos de Minecraft** (compatible con *Create*, *Sophisticated Storage*, *Lootr*, *Essential QoL*) como para **estudiantes y programadores** que desean poner en práctica consultas SQL (`SELECT`, `INSERT INTO`, `UPDATE`, `DELETE`, `WHERE`, `JOIN`, `GROUP BY`, `CREATE VIEW`, `CREATE TRIGGER`) dentro del juego.

---

### 🧰 Las 11 Herramientas y Bloques del Mod

#### 🖥️ Bloques Principales
1. **Terminal SQL (`sql_terminal`):** El servidor central. Vincula cofres con nombres personalizados (tablas), ejecuta consultas SQL múltiples separadas por `;` y evalúa tus `CREATE TRIGGER` automáticamente cada vez que cambia un inventario.
2. **Caja de Salida SQL (`sql_output_box`):** Inventario de recepción donde llegan los ítems que consultas con `SELECT`. Guarda el origen de cada ítem para que puedas devolverlos con `ROLLBACK`.
3. **Terminal SQL Portátil (`portable_terminal`):** Terminal de campo que funciona con una **Llave de Red** para consultar hasta 128 cofres infectados a la vez. Conserva sus datos internos al romperse.
4. **Reciclador Transmutador (`reciclador`):** Convierte materiales sobrantes en **Puntos SQL** canjeables por hierro, oro, diamantes, esmeraldas o netherite.

#### 🛠️ Herramientas de Red y Diagnóstico SQL
1. 📱 **Terminal Inalámbrica de Bolsillo (`wireless_terminal`):**
   * Haz `Shift + Clic Derecho` en tu Terminal Portátil para enlazarla y ábrela desde cualquier lugar del mundo haciendo `Clic Derecho` en el aire.
2. 🎯 **Vinculador Láser a Distancia (`laser_chest_linker`):**
   * Apunta a cofres hasta a **32 bloques de distancia** con un rayo láser de partículas. Haz `Shift + Clic Derecho` para ponerle nombre de tabla y `Clic Derecho` en la Terminal SQL para registrarlo.
3. 🦠 **Catalizador de Infección (`infection_catalyst`):**
   * Infecta automáticamente todos los cofres en un **radio de 52 bloques** (hasta **128 cofres** por red, saltando muros de hasta 12 bloques) y te entrega su **Llave de Red**.
4. 📡 **Repetidor de Expansión de Red (`infection_relay`):**
   * ¿Construiste más cofres lejos? Haz `Shift + Clic Derecho` en un cofre infectado y luego `Clic Derecho` en los cofres nuevos: expande tu misma red sin cambiar de llave.
5. 🔑 **Llavero Multirred (`network_key_ring`):**
   * Guarda hasta **8 Llaves de Red** distintas (*Red Base*, *Red Mina*, *Red Granja*). Con la llave en tu mano secundaria haz `Clic Derecho` para meterla al llavero, y `Shift + Clic Derecho` para cambiar de red activa.
6. 🧪 **Purificador de Infección (`infection_purifier`):**
   * Desinfecta y retira un cofre de la red con un `Clic Derecho` sin romper el cofre ni perder sus ítems.
7. 👓 **Visor HUD de Red (`network_visor`):**
   * Escanea tu red con un pulso de sonar: ilumina los cofres cercanos con partículas y te muestra en pantalla cuántos cofres están en chunks cargados (`Online`) o descargados (`Offline`).
8. 🔍 **Inspector y Diagnóstico SQL (`sql_inspector`):**
   * ¡Tu asistente para aprender SQL! Haz `Clic Derecho` sobre cualquier cofre y te mostrará en el chat un reporte holográfico con las columnas `WHERE` exactas que contienen sus ítems (`es_mineral`, `es_arma`, `es_armadura`, `es_herramienta`, `es_comida`, etc.).
9. 💾 **Tarjeta de Script SQL (`query_card`):**
   * Funciona como un *Stored Procedure* portátil. Haz `Shift + Clic Derecho` en una terminal para grabar su última consulta y `Clic Derecho` en otra terminal para ejecutarla al instante.

---

### 📚 Guía Rápida de Comandos SQL Soportados

#### 1. Consultas y Extracción (`SELECT`)
```sql
-- Extraer todos los diamantes de cualquier cofre hacia la Output Box
SELECT * FROM * WHERE nombre LIKE '%diamond%'

-- Extraer armas o armaduras dañadas ordenadas por daño
SELECT * FROM Armamento WHERE (es_arma = true OR es_armadura = true) AND dano > 0 ORDER BY dano DESC LIMIT 10

-- Contar y agrupar cuántos ítems hay por cada cofre
SELECT tabla, SUM(cantidad) FROM * GROUP BY tabla
```

#### 2. Transferencias Directas entre Cofres (`INSERT INTO`)
```sql
-- Mover todos los minerales desde OutputBox hacia el cofre Minerales
INSERT INTO Minerales SELECT * FROM OutputBox WHERE es_mineral = true

-- Mover de múltiplos de 9 en 9 (ideal para empaquetadoras de bloques de minerales)
INSERT INTO EntEmp SELECT * FROM Minerales WHERE es_mineral = true AND es_bloque = false AND multiplo_de = 9
```

#### 3. Automatización con Triggers Reactivos (`CREATE TRIGGER`)
Puedes ejecutar varios triggers en un solo script separándolos por punto y coma (`;`):

```sql
-- Script Maestro Clasificador Automático (de ServOut a tus cofres específicos)
CREATE TRIGGER ServMinerales ON ServOut WHERE es_mineral = true DO INSERT INTO Minerales;
CREATE TRIGGER ServLibros ON ServOut WHERE es_libro = true DO INSERT INTO Libros;
CREATE TRIGGER ServComida ON ServOut WHERE es_comida = true OR es_pocion = true DO INSERT INTO ComidaYPociones;
CREATE TRIGGER ServArmas ON ServOut WHERE es_arma = true OR es_armadura = true OR es_herramienta = true OR nombre LIKE '%horse_armor%' DO INSERT INTO Armamento;
CREATE TRIGGER ServBloques ON ServOut WHERE (es_bloque = true OR nombre LIKE '%sapling%') AND es_mineral = false DO INSERT INTO Bloques
```

```sql
-- Ciclo Automático de Compactación de Minerales (envía de 9 en 9 al compactador y devuelve bloques)
CREATE TRIGGER EntEmpMine ON Minerales WHERE (nombre LIKE '%ingot%' OR nombre LIKE '%nugget%' OR nombre LIKE '%diamond%' OR nombre LIKE '%gem%') AND es_bloque = false AND multiplo_de = 9 DO INSERT INTO EntEmp;
CREATE TRIGGER SalEmpMine ON SalEmp WHERE nombre LIKE '%block%' OR es_mineral = true DO INSERT INTO Minerales
```

```sql
-- Filtro de Granja de Mobs hacia Desmantelador / Reciclador
CREATE TRIGGER GranjArmaDes ON Granja WHERE es_arma = true OR es_armadura = true OR es_herramienta = true OR nombre = 'bone' DO INSERT INTO EntDes
```

#### 4. Gestión de Triggers, Vistas y Mantenimiento
```sql
SHOW TABLES                          -- Ver todos los cofres vinculados y su estado [ONLINE]/[OFFLINE]
SHOW TRIGGERS                        -- Listar todos los triggers activos en la terminal
DROP TRIGGER <nombre>                -- Eliminar un trigger específico
OPTIMIZE TABLE *                     -- Compactar stacks incompletos y ordenar todos los cofres
ALTER TABLE Minerales ORDER BY cantidad DESC -- Ordenar físicamente un cofre de mayor a menor cantidad
ROLLBACK                             -- Devolver todos los ítems de la Output Box a sus cofres de origen
```

---

### 🛠️ Compilación e Instalación para Desarrolladores

1. Clona el repositorio:
   ```bash
   git clone https://github.com/TU_USUARIO/sqlmod.git
   cd sqlmod
   ```
2. Compila el proyecto con Gradle (Java 17):
   ```bash
   ./gradlew build
   ```
3. El archivo `.jar` compilado estará listo en `build/libs/sqlmod-1.1.0.jar`.

---

### 📜 Licencia y Créditos
Desarrollado por **Rojoh** para **Minecraft 1.20.1 (Forge)**. ¡Aprende SQL mientras automatizas tu mundo!
