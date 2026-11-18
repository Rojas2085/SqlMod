---
trigger: always_on
description: Reglas críticas de arquitectura, seguridad de inventarios, motor SQL y estética para el proyecto SQL Storage Mod (sqlmod) en Minecraft Forge 1.20.1.
---

# Reglas de Arquitectura y Seguridad — `sqlmod` (Forge 1.20.1)

1. **Integridad de Ítems (Cero Duplicación / Cero Pérdida):**
   - Nunca extraigas ítems de un `IItemHandler` sin simular antes la inserción en el inventario destino (`insertItemStacked(..., true)`).
   - Si un bloque guarda su NBT manualmente al romperse en `playerWillDestroy` (ej. `PortableTerminalBlock`), su tabla de botín en `data/sqlmod/loot_tables/blocks/` debe tener `"pools": []` para evitar duplicación en modo Supervivencia.
   - Si un bloque permite desvincular o retirar un ítem insertado (como `NetworkKeyItem`), devuelve siempre el `ItemStack` con su NBT original al jugador (`player.getInventory().placeItemBackInInventory(...)`).

2. **Seguridad de Chunks y Servidor:**
   - Verifica siempre `level.isLoaded(pos)` antes de acceder a un cofre vinculado o infectado.
   - Si el cofre destino está en un chunk descargado durante una transferencia, encola los ítems en `VirtualDeliverySystem`.
   - Accede siempre a `InfectionNetworkManager` usando `serverLevel.getServer().overworld()` para mantener una única fuente de verdad entre dimensiones.

3. **Consistencia de Texturas y Recursos:**
   - Todas las texturas de ítems (`assets/sqlmod/textures/item/*.png`) deben tener fondo 100% transparente (`RGBA(0,0,0,0)`).
   - Todas las texturas de bloques (`assets/sqlmod/textures/block/*.png`) deben seguir la rejilla pixel-art nítida de `256x256` (celdas de `16x16` píxeles sin desenfoque).
