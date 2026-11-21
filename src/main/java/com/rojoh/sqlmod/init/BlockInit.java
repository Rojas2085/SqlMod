package com.rojoh.sqlmod.init;

import com.rojoh.sqlmod.block.AuditMailboxBlock;
import com.rojoh.sqlmod.block.CapacitySensorBlock;
import com.rojoh.sqlmod.block.NetworkFirewallBlock;
import com.rojoh.sqlmod.block.PortableTerminalBlock;
import com.rojoh.sqlmod.block.RecicladorBlock;
import com.rojoh.sqlmod.block.SqlOutputBoxBlock;
import com.rojoh.sqlmod.block.SqlTerminalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockInit {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "sqlmod");

    public static final RegistryObject<Block> SQL_TERMINAL = BLOCKS.register("sql_terminal",
            () -> new SqlTerminalBlock(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283906_).m_60999_().m_60913_(3.0F, 6.0F)));

    public static final RegistryObject<Block> SQL_OUTPUT_BOX = BLOCKS.register("sql_output_box",
            () -> new SqlOutputBoxBlock(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283825_).m_60999_().m_60913_(2.5F, 2.5F)));

    public static final RegistryObject<Block> PORTABLE_TERMINAL = BLOCKS.register("portable_terminal",
            PortableTerminalBlock::new);

    public static final RegistryObject<Block> RECICLADOR = BLOCKS.register("reciclador",
            () -> new RecicladorBlock(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283906_).m_60999_().m_60913_(3.0F, 6.0F)));

    // New v1.2.0 Infrastructure Blocks
    public static final RegistryObject<Block> AUDIT_MAILBOX = BLOCKS.register("sql_audit_mailbox",
            () -> new AuditMailboxBlock(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283906_).m_60999_().m_60913_(3.0F, 6.0F)));

    public static final RegistryObject<Block> CAPACITY_SENSOR = BLOCKS.register("sql_capacity_sensor",
            () -> new CapacitySensorBlock(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283906_).m_60999_().m_60913_(3.0F, 6.0F)));

    public static final RegistryObject<Block> NETWORK_FIREWALL = BLOCKS.register("sql_firewall",
            () -> new NetworkFirewallBlock(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283906_).m_60999_().m_60913_(4.0F, 12.0F)));
}
