---
name: sql-command-extension
description: >-
  Guía paso a paso para agregar nuevos comandos SQL, cláusulas WHERE/ORDER BY,
  funciones de agregación, columnas virtuales o triggers al motor SqlEngine y
  ExecuteQueryC2SPacket de sqlmod.
---

# Skill: Extensión del Motor SQL (`sql-command-extension`)

Usa esta habilidad cuando necesites ampliar la sintaxis SQL soportada por el **Terminal SQL** (`SqlTerminalBlockEntity`) o el **Terminal SQL Portátil** (`PortableTerminalBlockEntity`).

## 1. Archivos Clave Involucrados
- `com/rojoh/sqlmod/sql/SqlEngine.java`: Motor principal para consultas sobre cofres vinculados (`SELECT`, `INSERT INTO`, `UPDATE`, `DELETE FROM`).
- `com/rojoh/sqlmod/sql/TriggerEngine.java` y `SqlTrigger.java`: Evaluación de reglas `CREATE TRIGGER` y ejecución reactiva por Redstone.
- `com/rojoh/sqlmod/network/packet/ExecuteQueryC2SPacket.java`: Punto de entrada en el servidor para las consultas enviadas desde cualquier terminal.
- `com/rojoh/sqlmod/client/gui/SqlTerminalScreen.java` y `PortableTerminalScreen.java`: Pantallas cliente (autocompletado `Tab` y botones rápidos).

## 2. Cómo Añadir un Comando de Diagnóstico o Consulta de Red
Si el comando no mueve ítems físicamente (por ejemplo, `SHOW TABLES`, `SHOW ITEMS`, `SCAN`, `SELECT COUNT(*)`, `EXPLAIN`, `DESCRIBE`):
1. En `ExecuteQueryC2SPacket.handle(...)`, normaliza la consulta:
   ```java
   String rawTrim = this.query == null ? "" : this.query.trim();
   if (rawTrim.endsWith(";")) rawTrim = rawTrim.substring(0, rawTrim.length() - 1).trim();
   String upper = rawTrim.toUpperCase(Locale.ROOT);
   ```
2. Distingue si `be instanceof PortableTerminalBlockEntity` (red de `InfectionNetworkManager`) o `be instanceof SqlTerminalBlockEntity` (mapa `getLinkedChests()`).
3. Devuelve el resultado formateado enviando `new QueryResponseS2CPacket(resultString)` al `ServerPlayer`.

## 3. Cómo Añadir una Nueva Columna Virtual en `WHERE` y `ORDER BY`
En `SqlEngine.java`:
1. Localiza `evaluateSingleCondition(RowMatch row, String cond)` y `sortMatches(List<RowMatch> matches, String orderBy)`.
2. Agrega el nuevo nombre de columna (en español e inglés para mantener compatibilidad bilingüe):
   - Ejemplo: `etiqueta` / `tag`, `max_stack`, `nbt`, `rareza` / `rarity`.
3. Inspecciona `row.stack` (`ItemStack`) y evalúa el operador (`=`, `!=`, `LIKE`, `>`, `<`, `>=`, `<=`).

## 4. Cómo Añadir Nuevos Botones Rápidos o Autocompletado
1. En `PortableTerminalScreen.java` o `SqlTerminalScreen.java`, añade el término al arreglo `SQL_KEYWORDS`.
2. Si es una acción frecuente en el Terminal Portátil, añade un `Button.builder(...)` en `init()` que inserte o ejecute la plantilla en `this.queryInput`.
