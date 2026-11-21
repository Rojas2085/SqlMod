package com.rojoh.sqlmod.init;

import com.rojoh.sqlmod.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "sqlmod");

    // Existing blocks/items
    public static final RegistryObject<Item> SQL_TERMINAL = ITEMS.register("sql_terminal",
            () -> new BlockItem(BlockInit.SQL_TERMINAL.get(), new Item.Properties()));

    public static final RegistryObject<Item> SQL_OUTPUT_BOX = ITEMS.register("sql_output_box",
            () -> new BlockItem(BlockInit.SQL_OUTPUT_BOX.get(), new Item.Properties()));

    public static final RegistryObject<Item> CHEST_LINKER = ITEMS.register("chest_linker",
            () -> new ChestLinkerItem(new Item.Properties().m_41487_(1)));

    public static final RegistryObject<Item> PORTABLE_TERMINAL = ITEMS.register("portable_terminal",
            () -> new BlockItem(BlockInit.PORTABLE_TERMINAL.get(), new Item.Properties()));

    public static final RegistryObject<Item> INFECTION_CATALYST = ITEMS.register("infection_catalyst",
            () -> new InfectionCatalystItem(new Item.Properties().m_41487_(16)));

    public static final RegistryObject<Item> NETWORK_KEY = ITEMS.register("network_key",
            () -> new NetworkKeyItem(new Item.Properties().m_41487_(1)));

    public static final RegistryObject<Item> RECICLADOR = ITEMS.register("reciclador",
            () -> new BlockItem(BlockInit.RECICLADOR.get(), new Item.Properties()));

    // New Advanced Tools
    public static final RegistryObject<Item> WIRELESS_TERMINAL = ITEMS.register("wireless_terminal",
            () -> new WirelessTerminalItem(new Item.Properties()));

    public static final RegistryObject<Item> INFECTION_RELAY = ITEMS.register("infection_relay",
            () -> new InfectionRelayItem(new Item.Properties()));

    public static final RegistryObject<Item> NETWORK_KEY_RING = ITEMS.register("network_key_ring",
            () -> new NetworkKeyRingItem(new Item.Properties()));

    public static final RegistryObject<Item> LASER_CHEST_LINKER = ITEMS.register("laser_chest_linker",
            () -> new LaserChestLinkerItem(new Item.Properties()));

    public static final RegistryObject<Item> INFECTION_PURIFIER = ITEMS.register("infection_purifier",
            () -> new InfectionPurifierItem(new Item.Properties()));

    public static final RegistryObject<Item> NETWORK_VISOR = ITEMS.register("network_visor",
            () -> new NetworkVisorItem(new Item.Properties()));

    public static final RegistryObject<Item> SQL_INSPECTOR = ITEMS.register("sql_inspector",
            () -> new SqlInspectorItem(new Item.Properties()));

    public static final RegistryObject<Item> QUERY_CARD = ITEMS.register("query_card",
            () -> new QueryCardItem(new Item.Properties()));

    // New v1.2.0 Blocks & Manual
    public static final RegistryObject<Item> AUDIT_MAILBOX = ITEMS.register("sql_audit_mailbox",
            () -> new BlockItem(BlockInit.AUDIT_MAILBOX.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAPACITY_SENSOR = ITEMS.register("sql_capacity_sensor",
            () -> new BlockItem(BlockInit.CAPACITY_SENSOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> NETWORK_FIREWALL = ITEMS.register("sql_firewall",
            () -> new BlockItem(BlockInit.NETWORK_FIREWALL.get(), new Item.Properties()));

    public static final RegistryObject<Item> SQL_MANUAL = ITEMS.register("sql_manual",
            () -> new SqlManualItem(new Item.Properties()));
}
