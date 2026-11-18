# AGENTS.md — Guía Maestra de Arquitectura y Reglas para Agentes de IA (`sqlmod`)

Este documento define el contexto técnico, la arquitectura interna, las reglas de seguridad de ítems y las convenciones de desarrollo de **SQL Storage Mod (`sqlmod`)** para **Minecraft 1.20.1 (Forge 47.4.20, Java 17)**. Cualquier agente de IA que modifique o extienda este proyecto debe cumplir estrictamente estas directrices.

---

## 1. Resumen del Proyecto y Metadatos

- **Mod ID:** `sqlmod`
- **Paquete Base:** `com.rojoh.sqlmod`
- **Versión de Minecraft / Forge:** `1.20.1` / `47.4.20` (`Java 17`)
- **Propósito:** Gestión, consulta, filtrado, inserción, reciclaje y automatización de inventarios (cofres locales y redes globales de cofres infectados) mediante sentencias **SQL** en tiempo real y triggers ligados a Redstone.

---

## 2. Mapa de Arquitectura del Código Fuente

### Inicialización (`com.rojoh.sqlmod.init`)
- `BlockInit`: Registra los bloques del mod:
  - `sql_terminal` (`SqlTerminalBlock`)
  - `sql_output_box` (`SqlOutputBoxBlock`)
  - `reciclador` (`RecicladorBlock`)
  - `portable_terminal` (`PortableTerminalBlock`)
- `ItemInit`: Registra BlockItems e ítems de herramientas/red:
  - `chest_linker` (`ChestLinkerItem`)
  - `infection_catalyst` (`InfectionCatalystItem`)
  - `network_key` (`NetworkKeyItem`)
- `BlockEntityInit`: Registra `SqlTerminalBlockEntity`, `SqlOutputBoxBlockEntity`, `RecicladorBlockEntity`, `PortableTerminalBlockEntity`.
- `MenuInit`: Registra los contenedores (`SqlTerminalMenu`, `SqlOutputBoxMenu`, `RecicladorMenu`, `PortableTerminalMenu`).
- `CreativeTabInit`: Pestaña creativa dedicada del mod (`sqlmod_tab`).

### Motor SQL y Triggers (`com.rojoh.sqlmod.sql`)
- `SqlEngine`: Analizador léxico/sintáctico mediante expresiones regulares y ejecutor sobre `IItemHandler`.
  - Soporta `SELECT`, `INSERT INTO`, `UPDATE`, `DELETE FROM`, `SHOW TABLES`, `SHOW ITEMS`, `SCAN`, `HELP`.
  - Cláusulas soportadas: `WHERE` (`=`, `!=`, `>`, `<`, `>=`, `<=`, `LIKE`, `AND`, `OR`), `ORDER BY [ASC|DESC]`, `LIMIT`.
  - Columnas virtuales de cada slot:
    - `item_id` (ej. `minecraft:diamond`)
    - `nombre` / `name` (nombre visible o path del registro)
    - `cantidad` / `count` (tamaño del stack)
    - `slot` (índice numérico del slot)
    - `dano` / `damage` (daño actual de la herramienta/armadura)
    - `encantado` / `enchanted` (`true`/`false` o `1`/`0`)
    - `mod` (namespace del ítem, ej. `minecraft`, `sqlmod`)
    - `tabla` / `cofre` (nombre asignado al cofre vinculado)
- `SqlTrigger` y `TriggerEngine`:
  - Permiten registrar reglas reactivas (`CREATE TRIGGER <nombre> WHEN COUNT(<condicion>) > N THEN <sentencia_sql>`).
  - Se evalúan al recibir pulsos de Redstone o eventos de actualización en el `SqlTerminalBlockEntity`.
- `DatabaseManager`: Capa auxiliar para consultas y metadatos estructurados.

### Datos Persistentes de Mundo (`com.rojoh.sqlmod.world.data`)
- `InfectionNetworkManager` (`SavedData` global del `ServerLevel` Overworld):
  - Administra redes de cofres infectados identificadas por un `UUID` (`NetworkId`).
  - Cada `InfectionNetwork` almacena un `Set<BlockPos> chestPositions` de los cofres infectados con `InfectionCatalystItem`.
  - **Regla crítica:** Siempre acceder a través de `serverLevel.getServer().overworld()` para que la red sea consistente entre lecturas.
- `VirtualDeliverySystem` (`SavedData`):
  - Cola de entregas diferidas para cofres que se encuentran en chunks descargados (`!level.isLoaded(pos)`).
  - Cuando el chunk del cofre destino se carga o se interactúa con él, entrega los `ItemStack` pendientes sin perder ni duplicar ítems.

### Compatibilidad con Inventarios y Lootr (`com.rojoh.sqlmod.util` y `compat`)
- `InventoryHelper`: Punto único de acceso para obtener el `LazyOptional<IItemHandler>` de cualquier bloque contenedor (`ChestBlock`, barriles, cofres dobles o contenedores de mods).
- `LootrCompat`: Detecta cofres del mod **Lootr** por reflexión/capacidades sin requerir dependencia dura en compilación, resolviendo el inventario instanciado por jugador (`ServerPlayer`).

### Red Cliente-Servidor (`com.rojoh.sqlmod.network`)
- `SqlModPacketHandler`: Canal `SimpleChannel` con protocolo `"1"`.
- Paquetes `C2S` (Cliente -> Servidor):
  - `ExecuteQueryC2SPacket`: Ejecuta consultas tanto en `SqlTerminalBlockEntity` como en `PortableTerminalBlockEntity` (según el `BlockEntity` en `BlockPos`).
  - `LinkChestC2SPacket`: Asigna el alias/nombre de tabla al cofre vinculado con `ChestLinkerItem`.
  - `ReturnItemsC2SPacket`: Devuelve ítems desde la `SqlOutputBoxBlockEntity` a sus cofres de origen.
  - `RecyclerPurchaseC2SPacket`: Canjea puntos de reciclaje por ítems generados en el `RecicladorBlockEntity`.
  - `RequestNetworkStatusC2SPacket`: Solicita estado de chunks cargados/descargados de los cofres vinculados.
- Paquetes `S2C` (Servidor -> Cliente):
  - `QueryResponseS2CPacket`: Envía el log de salida de la consulta SQL a la pantalla abierta (`SqlTerminalScreen` o `PortableTerminalScreen`).
  - `SyncNetworkStatusS2CPacket`: Sincroniza la lista de cofres vinculados y su estado online/offline.
  - `SyncTriggersS2CPacket`: Sincroniza la lista de triggers activos.
  - `SyncRecyclerPointsS2CPacket`: Sincroniza los puntos acumulados en el Reciclador.

---

## 3. Reglas Inquebrantables para Futuras Implementaciones

### Regla 1: Cero Duplicación y Cero Pérdida de Ítems (Atomicidad)
1. **Extracción e Inserción Simulada:** Antes de extraer un `ItemStack` de un cofre origen (`handler.extractItem(slot, count, false)`), **SIEMPRE** simula la inserción en el destino (`ItemHandlerHelper.insertItemStacked(destHandler, extractedSim, true)`).
2. Solo extrae realmente la cantidad que el inventario destino aceptó en la simulación (`accepted = toExtract - remainder.getCount()`).
3. **Preservación de NBT al Romper Bloques:** Si un bloque guarda estado en su ítem al romperse en `playerWillDestroy` (como `PortableTerminalBlock`), su archivo `data/sqlmod/loot_tables/blocks/<block>.json` debe tener `"pools": []` para evitar que suelte un segundo bloque vacío duplicado en supervivencia.
4. **Devolución de Ítems Vinculados:** Cualquier acción de desvinculación (`Shift + Clic Derecho`) que limpie una llave o ítem insertado en un bloque debe devolver el ítem con su NBT original al inventario del jugador (`player.getInventory().placeItemBackInInventory(stack)`).

### Regla 2: Validación en el Servidor (`Server-Side Authority`)
1. Nunca confíes ciegamente en coordenadas o cantidades enviadas desde el cliente en paquetes `C2S`.
2. Verifica siempre en el servidor:
   - Que el `ServerPlayer` esté dentro del rango de interacción o tenga acceso legítimo al `BlockEntity`.
   - Que `level.isLoaded(pos)` sea `true` antes de llamar a `level.getBlockEntity(pos)`. Si el chunk no está cargado, usa `VirtualDeliverySystem` o reporta el cofre como `[OFFLINE]`.

### Regla 3: Estándar Visual de Texturas y GUI
1. **Bloques:** Texturas de `256x256` píxeles generadas sobre una **rejilla estricta de 16x16 celdas** (16x16 píxeles por celda, `NearestNeighbor`, sin desenfoque gaussiano) con marco metálico industrial oscuro (`#181A22`), acentos carmesí (`#A02020`) y pantallas CRT verde fósforo (`#28E050`) o cian (`#00DCE6`).
2. **Ítems:** Texturas de `32x32` píxeles con fondo **100% transparente (`RGBA(0,0,0,0)`)** y borde pixel-art oscuro definido. Nunca usar fondos blancos o grises sólidos en texturas de `assets/sqlmod/textures/item/`.
3. **GUIs (`Screen`):** Soportar siempre scroll con rueda del ratón (`mouseScrolled`), autocompletado con tecla `Tab` (`keyPressed`) y renderizado recortado a la caja de consola.

### Regla 4: Compilación y Despliegue
- Cuando se compile mediante Gradle (`./gradlew build`) o compilación incremental con `javac` sobre los mappings SRG de Forge `1.20.1-47.4.20`, recuerda que en bytecode SRG los métodos de Minecraft usan nombres `m_XXXXX_` / `f_XXXXX_` mientras que los métodos propios del mod y de Forge (`IItemHandler`, `ForgeRegistries`) conservan sus nombres descriptivos.
- Siempre verifica el contenido del `.jar` resultante con `jar tf` y exporta la versión final probada a `build/libs/sqlmod-1.0.0.jar`.
