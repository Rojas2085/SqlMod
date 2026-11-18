---
name: new-block-or-terminal
description: >-
  Procedimiento completo para crear y registrar nuevos bloques, BlockEntities,
  menús contenedores, pantallas GUI, paquetes de red y recursos en sqlmod (Forge 1.20.1).
---

# Skill: Creación de Nuevos Bloques, Terminales y Periféricos (`new-block-or-terminal`)

Usa esta habilidad al añadir nuevos bloques o dispositivos a la red SQL (ej. *SQL Autocrafter*, *SQL Importer/Exporter*, *SQL Server Rack*).

## 1. Registro en `com.rojoh.sqlmod.init`
1. **`BlockInit.java`**: Registra el `RegistryObject<Block>` en `BLOCKS`.
2. **`ItemInit.java`**: Registra el `BlockItem` correspondiente en `ITEMS`.
3. **`BlockEntityInit.java`**: Registra el `BlockEntityType<T>` vinculándolo al bloque de `BlockInit`.
4. **`MenuInit.java`**: Si tiene interfaz gráfica, registra el `MenuType<T>` usando `IForgeMenuType.create(...)`.
5. **`CreativeTabInit.java`**: Añade el ítem al `CreativeModeTab` del mod.

## 2. Implementación de `Block` y `BlockEntity`
- **Persistencia NBT en el Item al romper (`playerWillDestroy`):**
  - Si el bloque guarda configuración de red (`NetworkId`, `OutputBoxPos`, `LastQuery`, puntos, etc.), sobrescribe `playerWillDestroy` para generar un `ItemEntity` con `stack.addTagElement("BlockEntityTag", tag)` o tags personalizados, y deja `data/sqlmod/loot_tables/blocks/<id>.json` con `"pools": []`.
- **Soporte de Redstone (`neighborChanged`):**
  - Para bloques automatizables, detecta `level.hasNeighborSignal(pos)` y usa una bandera booleana `wasPowered` en el `BlockEntity` para disparar la acción solo en el flanco de subida del pulso de Redstone.

## 3. Registro de Pantalla y Red
- Registra el `Screen` en `ClientModEvents.onClientSetup` mediante `MenuScreens.register(...)`.
- Si requiere sincronización o acciones desde la GUI, crea el paquete en `com.rojoh.sqlmod.network.packet` y regístralo en `SqlModPacketHandler.register()`.
