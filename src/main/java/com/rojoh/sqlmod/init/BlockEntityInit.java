package com.rojoh.sqlmod.init;

import com.rojoh.sqlmod.block.entity.AuditMailboxBlockEntity;
import com.rojoh.sqlmod.block.entity.CapacitySensorBlockEntity;
import com.rojoh.sqlmod.block.entity.NetworkFirewallBlockEntity;
import com.rojoh.sqlmod.block.entity.PortableTerminalBlockEntity;
import com.rojoh.sqlmod.block.entity.RecicladorBlockEntity;
import com.rojoh.sqlmod.block.entity.SqlOutputBoxBlockEntity;
import com.rojoh.sqlmod.block.entity.SqlTerminalBlockEntity;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntityInit {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "sqlmod");

    public static final RegistryObject<BlockEntityType<SqlTerminalBlockEntity>> SQL_TERMINAL =
            BLOCK_ENTITIES.register("sql_terminal",
                    () -> createType(SqlTerminalBlockEntity::new, BlockInit.SQL_TERMINAL.get()));

    public static final RegistryObject<BlockEntityType<SqlOutputBoxBlockEntity>> SQL_OUTPUT_BOX =
            BLOCK_ENTITIES.register("sql_output_box",
                    () -> createType(SqlOutputBoxBlockEntity::new, BlockInit.SQL_OUTPUT_BOX.get()));

    public static final RegistryObject<BlockEntityType<PortableTerminalBlockEntity>> PORTABLE_TERMINAL =
            BLOCK_ENTITIES.register("portable_terminal",
                    () -> createType(PortableTerminalBlockEntity::new, BlockInit.PORTABLE_TERMINAL.get()));

    public static final RegistryObject<BlockEntityType<RecicladorBlockEntity>> RECICLADOR =
            BLOCK_ENTITIES.register("reciclador",
                    () -> createType(RecicladorBlockEntity::new, BlockInit.RECICLADOR.get()));

    // New v1.2.0 Block Entities
    public static final RegistryObject<BlockEntityType<AuditMailboxBlockEntity>> AUDIT_MAILBOX =
            BLOCK_ENTITIES.register("sql_audit_mailbox",
                    () -> createType(AuditMailboxBlockEntity::new, BlockInit.AUDIT_MAILBOX.get()));

    public static final RegistryObject<BlockEntityType<CapacitySensorBlockEntity>> CAPACITY_SENSOR =
            BLOCK_ENTITIES.register("sql_capacity_sensor",
                    () -> createType(CapacitySensorBlockEntity::new, BlockInit.CAPACITY_SENSOR.get()));

    public static final RegistryObject<BlockEntityType<NetworkFirewallBlockEntity>> NETWORK_FIREWALL =
            BLOCK_ENTITIES.register("sql_firewall",
                    () -> createType(NetworkFirewallBlockEntity::new, BlockInit.NETWORK_FIREWALL.get()));

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> BlockEntityType<T> createType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        try {
            Class<?> supplierIface = Class.forName("net.minecraft.world.level.block.entity.BlockEntityType$BlockEntitySupplier");
            Object proxy = Proxy.newProxyInstance(
                    supplierIface.getClassLoader(),
                    new Class<?>[]{supplierIface},
                    (p, method, args) -> {
                        if (args != null && args.length == 2 && args[0] instanceof BlockPos bp && args[1] instanceof BlockState bs) {
                            return factory.apply(bp, bs);
                        }
                        return null;
                    }
            );
            Method ofMethod = BlockEntityType.Builder.class.getMethod("m_155273_", supplierIface, Block[].class);
            BlockEntityType.Builder<T> builder = (BlockEntityType.Builder<T>) ofMethod.invoke(null, proxy, blocks);
            return builder.m_58966_(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register BlockEntityType", e);
        }
    }
}
