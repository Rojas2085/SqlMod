package com.rojoh.sqlmod.block;

import com.rojoh.sqlmod.block.entity.CapacitySensorBlockEntity;
import com.rojoh.sqlmod.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class CapacitySensorBlock extends BaseEntityBlock {

    public CapacitySensorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape m_7514_(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity m_142194_(BlockPos pos, BlockState state) {
        return new CapacitySensorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.m_5776_()) return null;
        return m_152132_(type, BlockEntityInit.CAPACITY_SENSOR.get(), (lvl, p, st, be) -> be.serverTick());
    }

    @Override
    public boolean m_7899_(BlockState state) { // isSignalSource
        return true;
    }

    @Override
    public int m_6378_(BlockState state, BlockGetter level, BlockPos pos, Direction dir) { // getSignal
        BlockEntity be = level.m_7702_(pos);
        if (be instanceof CapacitySensorBlockEntity sensor) {
            return sensor.getRedstonePower();
        }
        return 0;
    }

    @Override
    public boolean m_7278_(BlockState state) { // hasAnalogOutputSignal
        return true;
    }

    @Override
    public int m_6782_(BlockState state, Level level, BlockPos pos) { // getAnalogOutputSignal
        BlockEntity be = level.m_7702_(pos);
        if (be instanceof CapacitySensorBlockEntity sensor) {
            return sensor.getRedstonePower();
        }
        return 0;
    }

    @Override
    public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.m_5776_()) return InteractionResult.SUCCESS;

        BlockEntity be = level.m_7702_(pos);
        if (be instanceof CapacitySensorBlockEntity sensor) {
            sensor.recalculate();
            int pct = sensor.getPercent();
            String color = pct >= 85 ? "§c" : (pct >= 50 ? "§e" : "§a");
            String msg = "§b📊 Sensor de Capacidad: " + color + pct + "% §7(Redstone: §f" + sensor.getRedstonePower()
                    + "/15§7 | Slots: §f" + sensor.getLastUsedSlots() + "/" + sensor.getLastTotalSlots() + "§7)";
            player.m_5661_(Component.m_237113_(msg), true);
        }
        return InteractionResult.CONSUME;
    }
}
