# SKILLS.md — Catálogo de Habilidades y Procedimientos Técnicos (`sqlmod`)

Este documento recopila los procedimientos paso a paso (*Skills*) para extender, depurar, diseñar texturas y compilar nuevas funcionalidades en **SQL Storage Mod (`sqlmod`)** para Minecraft Forge 1.20.1.

> **Nota para Agentes de IA (Antigravity):** Además de este índice maestro, cada habilidad está registrada modularmente dentro de `.agents/skills/<nombre>/SKILL.md` y las reglas de arquitectura están activas en `.agents/rules/forge-sqlmod-architecture.md`.

---

## Índice de Skills del Proyecto

1. [**Skill 1: Extensión del Motor SQL y Nuevos Comandos (`sql-command-extension`)**](#1-skill-extensión-del-motor-sql-y-nuevos-comandos)
2. [**Skill 2: Creación de Nuevos Bloques, Terminales y Paquetes de Red (`new-block-or-terminal`)**](#2-skill-creación-de-nuevos-bloques-terminales-y-paquetes-de-red)
3. [**Skill 3: Generación de Texturas Pixel-Grid y Modelos (`pixel-grid-texture-gen`)**](#3-skill-generación-de-texturas-pixel-grid-y-modelos)
4. [**Skill 4: Compilación, Parcheo SRG y Exportación a CurseForge (`build-and-deploy`)**](#4-skill-compilación-parcheo-srg-y-exportación-a-curseforge)

---

## 1. Skill: Extensión del Motor SQL y Nuevos Comandos
**Ruta modular:** `.agents/skills/sql-command-extension/SKILL.md`

### Cuándo usarla
Cuando se desee añadir:
- Nuevas sentencias SQL (ej. `GROUP BY`, `ALTER TABLE`, `JOIN`, `EXPORT`, `CRAFT`).
- Nuevas funciones de agregación (`SUM`, `MIN`, `MAX`, `AVG`).
- Nuevas columnas virtuales para filtrar ítems (ej. `nbt`, `durabilidad_max`, `rareza`, `etiqueta` / `tag`).
- Nuevos comandos rápidos en el **Terminal SQL** o en el **Terminal SQL Portátil**.

### Procedimiento Paso a Paso
1. **Evaluar el alcance del comando:**
   - Si el comando es una consulta rápida de metadatos o red (`SHOW ...`, `SCAN`, `HELP`, `SELECT COUNT(*) FROM infectados`), interceptarlo en `ExecuteQueryC2SPacket.handle(...)` antes de invocar la extracción física de ítems.
   - Si el comando transforma o filtra slots de `IItemHandler`, implementarlo en `SqlEngine.java`.
2. **Añadir una nueva columna virtual de filtrado (`SqlEngine.evaluateSingleCondition`):**
   - En `SqlEngine.evaluateSingleCondition(RowMatch row, String cond)`, extrae la propiedad desde `row.stack` (`ItemStack`):
     ```java
     // Ejemplo: filtrar por rareza o encantabilidad
     case "rareza":
     case "rarity":
         String rarity = row.stack.getRarity().name().toLowerCase(Locale.ROOT);
         return compareString(rarity, op, val);
     ```
3. **Respetar la simulación de transferencia:**
   - Todo movimiento de ítems entre `sourceHandler` y `destHandler` debe seguir el patrón de 2 fases:
     1. `ItemStack sim = sourceHandler.extractItem(slot, amount, true);`
     2. `ItemStack rem = ItemHandlerHelper.insertItemStacked(destHandler, sim, true);`
     3. `int accepted = sim.getCount() - rem.getCount();`
     4. Si `accepted > 0`, ejecutar `extractItem(slot, accepted, false)` seguido de `insertItemStacked(destHandler, realExtracted, false)`.
4. **Actualizar el autocompletado en Cliente:**
   - Añade las nuevas palabras clave o columnas a los arreglos `SQL_KEYWORDS` en `SqlTerminalScreen.java` y `PortableTerminalScreen.java` para que la tecla `Tab` las sugiera automáticamente.

---

## 2. Skill: Creación de Nuevos Bloques, Terminales y Paquetes de Red
**Ruta modular:** `.agents/skills/new-block-or-terminal/SKILL.md`

### Cuándo usarla
Cuando se agregue un nuevo periférico a la red SQL (ej. *SQL Importer*, *SQL Autocrafter*, *Wireless Relay*, *SQL Security Firewall*).

### Checklist de Archivos a Modificar/Crear
1. **Bloque y BlockEntity:**
   - Crear `<Nombre>Block.java` en `com.rojoh.sqlmod.block` (extender `Block` e implementar `EntityBlock`).
   - Crear `<Nombre>BlockEntity.java` en `com.rojoh.sqlmod.block.entity` (implementar `MenuProvider` si tiene GUI, y sobrescribir `saveAdditional` y `load` para persistencia NBT).
   - Si el bloque conserva su NBT al romperse en `playerWillDestroy`, configurar `data/sqlmod/loot_tables/blocks/<id>.json` con `"pools": []`.
2. **Registros (`com.rojoh.sqlmod.init`):**
   - Registrar el bloque en `BlockInit.BLOCKS`.
   - Registrar su `BlockItem` en `ItemInit.ITEMS`.
   - Registrar su `BlockEntityType` en `BlockEntityInit.BLOCK_ENTITIES`.
   - Añadirlo a `CreativeTabInit` para que aparezca en la pestaña creativa del mod.
3. **Menú y Pantalla (si tiene GUI):**
   - Crear `<Nombre>Menu.java` y registrarlo en `MenuInit.MENUS`.
   - Crear `<Nombre>Screen.java` y vincularlo con `MenuScreens.register(...)` dentro de `ClientModEvents.onClientSetup`.
4. **Paquetes de Red (`SqlModPacketHandler`):**
   - Cada nuevo paquete debe implementar `encode(FriendlyByteBuf)`, constructor `(FriendlyByteBuf)` y `handle(Supplier<NetworkEvent.Context>)`.
   - Incrementar `id++` en `SqlModPacketHandler.register()`.
5. **Recursos y Localización:**
   - Añadir entradas en `assets/sqlmod/blockstates/<id>.json`, `models/block/<id>.json`, `models/item/<id>.json`.
   - Añadir traducciones en `assets/sqlmod/lang/es_es.json` y `assets/sqlmod/lang/en_us.json`.
   - Añadir receta de crafteo en `data/sqlmod/recipes/<id>.json`.

---

## 3. Skill: Generación de Texturas Pixel-Grid y Modelos
**Ruta modular:** `.agents/skills/pixel-grid-texture-gen/SKILL.md`

### Cuándo usarla
Cuando se creen o actualicen texturas de bloques, terminales o ítems del mod para mantener una identidad visual 100% coherente con `sql_terminal_front.png`.

### Reglas de Diseño Visual
1. **Texturas de Bloques (`256x256` en rejilla lógica de `16x16` celdas):**
   - Cada celda lógica mide exactamente `16x16` píxeles reales con bordes nítidos (sin interpolación bilineal).
   - **Paleta oficial del mod:**
     - Chasis exterior oscuro: `#181A22` / `#252834` / `#323646`
     - Refuerzos de esquina (Terminal / Portátil): `#A02020` / `#CC2D2D`
     - Pantalla CRT fondo: `#081610` / `#0E281C`
     - Texto y píxeles SQL brillantes: `#28E050` / `#78FF96`
     - Acentos inalámbricos / Red: `#00DCE6` (cian) y `#FFB400` (contactos de oro)
     - Acentos de infección: `#B428DC` / `#F05AFF` (plasma bio-digital)
2. **Texturas de Ítems (`32x32`):**
   - El canal alfa del fondo debe ser estrictamente `RGBA(0, 0, 0, 0)` (100% transparente).
   - Todo ítem debe tener un contorno oscuro (`#12141C`) de 1 píxel para destacar sobre cualquier slot del inventario de Minecraft.

---

## 4. Skill: Compilación, Parcheo SRG y Exportación a CurseForge
**Ruta modular:** `.agents/skills/build-and-deploy/SKILL.md`

### Cuándo usarla
Al compilar cambios en el código Java o recursos para generar el archivo `sqlmod-1.0.0.jar` listo para producción o pruebas en CurseForge.

### Opciones de Compilación
1. **Compilación Estándar con Gradle (cuando todos los archivos locales están hidratados):**
   ```powershell
   ./gradlew build
   ```
2. **Compilación Directa con `javac` sobre Caché SRG de Forge `1.20.1-47.4.20` (Rápida / Offline / OneDrive Deshidratado):**
   - Utiliza los artefactos cacheados en `C:\Users\rojoh\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.20\forge-1.20.1-47.4.20-srg.jar` y `binpatched.jar`.
   - Empaqueta con `jar cfm` incluyendo `META-INF/MANIFEST.MF` y copia el resultado a:
     - `C:\Users\rojoh\OneDrive\Desktop\sqlMod\build\libs\sqlmod-1.0.0.jar`
     - `C:\Users\rojoh\curseforge\minecraft\Instances\DungeonsPrub\mods\sqlmod-1.0.0.jar`
