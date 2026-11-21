package com.rojoh.sqlmod.block.entity;

import com.rojoh.sqlmod.init.BlockEntityInit;
import com.rojoh.sqlmod.util.InventoryHelper;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

public class CapacitySensorBlockEntity extends BlockEntity {

    private int tickCounter = 0;
    private int redstonePower = 0;
    private int lastUsedSlots = 0;
    private int lastTotalSlots = 0;

    public CapacitySensorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.CAPACITY_SENSOR.get(), pos, state);
    }

    public void serverTick() {
        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;
        recalculate();
    }

    public void recalculate() {
        if (this.f_58857_ == null || this.f_58857_.f_46443_) return;

        int used = 0;
        int total = 0;

        for (Direction dir : Direction.values()) {
            BlockPos adjPos = this.f_58858_.m_121945_(dir);
            if (!this.f_58857_.m_46749_(adjPos)) continue;
            BlockEntity be = this.f_58857_.m_7702_(adjPos);
            if (be == null) continue;

            if (be instanceof SqlTerminalBlockEntity terminal) {
                Map<String, BlockPos> chests = terminal.getLinkedChests();
                for (BlockPos cp : chests.values()) {
                    if (!this.f_58857_.m_46749_(cp)) continue;
                    BlockEntity cbe = this.f_58857_.m_7702_(cp);
                    if (cbe == null) continue;
                    LazyOptional<IItemHandler> cap = InventoryHelper.getInventory(this.f_58857_, cp, cbe, null);
                    if (cap.isPresent()) {
                        IItemHandler h = cap.orElseThrow(IllegalStateException::new);
                        int slots = h.getSlots();
                        total += slots;
                        for (int i = 0; i < slots; i++) {
                            if (!h.getStackInSlot(i).m_41619_()) used++;
                        }
                    }
                }
            } else {
                LazyOptional<IItemHandler> cap = InventoryHelper.getInventory(this.f_58857_, adjPos, be, null);
                if (cap.isPresent()) {
                    IItemHandler h = cap.orElseThrow(IllegalStateException::new);
                    int slots = h.getSlots();
                    total += slots;
                    for (int i = 0; i < slots; i++) {
                        if (!h.getStackInSlot(i).m_41619_()) used++;
                    }
                }
            }
        }

        this.lastUsedSlots = used;
        this.lastTotalSlots = total;

        int newPower = 0;
        if (total > 0 && used > 0) {
            newPower = Math.max(1, (int) Math.round(((double) used / total) * 15.0D));
        }

        if (newPower != this.redstonePower) {
            this.redstonePower = newPower;
            m_6596_();
            this.f_58857_.m_46672_(this.f_58858_, m_58900_().m_60734_());
        }
    }

    public int getRedstonePower() {
        return redstonePower;
    }

    public int getLastUsedSlots() {
        return lastUsedSlots;
    }

    public int getLastTotalSlots() {
        return lastTotalSlots;
    }

    public int getPercent() {
        if (lastTotalSlots <= 0) return 0;
        return (int) Math.round(((double) lastUsedSlots / lastTotalSlots) * 100.0D);
    }

    @Override
    protected void m_183515_(CompoundTag tag) {
        super.m_183515_(tag);
        tag.m_128405_("RedstonePower", redstonePower);
    }

    @Override
    public void m_142466_(CompoundTag tag) {
        super.m_142466_(tag);
        redstonePower = tag.m_128451_("RedstonePower");
    }
}
