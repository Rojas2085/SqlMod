package com.rojoh.sqlmod.item;

import com.rojoh.sqlmod.block.SqlTerminalBlock;
import com.rojoh.sqlmod.block.entity.SqlTerminalBlockEntity;
import com.rojoh.sqlmod.client.ClientModEvents;
import com.rojoh.sqlmod.util.InventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class LaserChestLinkerItem extends Item {
    private static final double RANGE = 32.0D;

    public LaserChestLinkerItem(Properties properties) {
        super(properties.m_41487_(1));
    }

    @Override
    public InteractionResult m_6225_(UseOnContext context) {
        Level level = context.m_43725_();
        Player player = context.m_43723_();
        BlockPos pos = context.m_8083_();
        BlockEntity be = level.m_7702_(pos);

        // If clicking on Terminal: link the currently selected laser chest
        if (be instanceof SqlTerminalBlockEntity terminal) {
            ItemStack stack = context.m_43722_();
            CompoundTag tag = stack.m_41783_();
            if (tag != null && tag.m_128441_("LaserChestPos") && tag.m_128441_("LaserChestName")) {
                if (!level.f_46443_) {
                    BlockPos chestPos = NbtUtils.m_129239_(tag.m_128469_("LaserChestPos"));
                    String chestName = tag.m_128461_("LaserChestName");
                    terminal.linkChest(chestName, chestPos);
                    level.m_5594_(null, pos, SoundEvents.f_11704_, SoundSource.PLAYERS, 1.0F, 1.4F);
                    if (player != null) {
                        player.m_5661_(Component.m_237113_("§a[Láser Linker] Cofre '" + chestName + "' (" + chestPos.m_123344_() + ") registrado en la Terminal SQL!"), true);
                    }
                    tag.m_128473_("LaserChestPos");
                    tag.m_128473_("LaserChestName");
                }
                return InteractionResult.m_19078_(level.f_46443_);
            }
        }

        return super.m_6225_(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.m_21120_(hand);

        Vec3 eyePos = player.m_20299_(1.0F);
        Vec3 look = player.m_20154_();
        Vec3 end = eyePos.m_82549_(look.m_82490_(RANGE));

        BlockHitResult hit = level.m_45547_(new ClipContext(eyePos, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.m_6662_() == HitResult.Type.BLOCK) {
            BlockPos targetPos = hit.m_82425_();
            BlockEntity be = level.m_7702_(targetPos);

            if (InventoryHelper.hasInventory(level, targetPos, be)) {
                if (level.f_46443_) {
                    if (player.m_6144_()) {
                        ClientModEvents.openChestNameScreen(targetPos);
                    }
                    return InteractionResultHolder.m_19098_(stack);
                }

                ServerLevel sl = (ServerLevel) level;
                Vec3 targetCenter = new Vec3(targetPos.m_123341_() + 0.5D, targetPos.m_123342_() + 0.5D, targetPos.m_123343_() + 0.5D);
                Vec3 ray = targetCenter.m_82546_(eyePos);
                int steps = (int) Math.max(5, Math.ceil(eyePos.m_82554_(targetCenter) * 2));
                for (int i = 0; i <= steps; i++) {
                    Vec3 pt = eyePos.m_82549_(ray.m_82490_((double) i / steps));
                    sl.m_8767_(ParticleTypes.f_123796_, pt.f_82479_, pt.f_82480_, pt.f_82481_, 1, 0, 0, 0, 0);
                }

                sl.m_8767_(ParticleTypes.f_123749_, targetPos.m_123341_() + 0.5D, targetPos.m_123342_() + 1.0D, targetPos.m_123343_() + 0.5D, 10, 0.2D, 0.2D, 0.2D, 0.05D);
                sl.m_5594_(null, player.m_20183_(), SoundEvents.f_11704_, SoundSource.PLAYERS, 0.8F, 2.0F);

                CompoundTag tag = stack.m_41784_();
                tag.m_128365_("LaserChestPos", NbtUtils.m_129224_(targetPos));
                if (stack.m_41788_()) {
                    tag.m_128359_("LaserChestName", stack.m_41786_().getString());
                }

                player.m_5661_(Component.m_237113_("§b[Láser Linker] Cofre fijado en " + targetPos.m_123344_() + " (a " + String.format("%.1f", eyePos.m_82554_(targetCenter)) + "m). Shift+Clic para nombrar."), true);
                return InteractionResultHolder.m_19090_(stack);
            }
        }

        return super.m_7203_(level, player, hand);
    }

    @Override
    public void m_7373_(ItemStack stack, Level level, List<Component> tooltips, TooltipFlag flag) {
        tooltips.add(Component.m_237113_("§7Vinculación remota por rayo láser (hasta 32m)."));
        CompoundTag tag = stack.m_41783_();
        if (tag != null && tag.m_128441_("LaserChestPos")) {
            BlockPos pos = NbtUtils.m_129239_(tag.m_128469_("LaserChestPos"));
            String name = tag.m_128461_("LaserChestName");
            tooltips.add(Component.m_237113_("§bFijado: §f" + pos.m_123344_() + (!name.isEmpty() ? " §7(Alias: §e" + name + "§7)" : "")));
            tooltips.add(Component.m_237113_("§eClic en Terminal SQL: Registrar cofre fijado"));
        } else {
            tooltips.add(Component.m_237113_("§7Apunta a un cofre lejano y haz Clic Derecho para fijarlo."));
            tooltips.add(Component.m_237113_("§8Shift+Clic Derecho para asignarle alias por pantalla."));
        }
        super.m_7373_(stack, level, tooltips, flag);
    }
}
