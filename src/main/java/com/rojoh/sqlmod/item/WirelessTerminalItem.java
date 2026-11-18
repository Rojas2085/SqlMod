package com.rojoh.sqlmod.item;

import com.rojoh.sqlmod.block.entity.PortableTerminalBlockEntity;
import com.rojoh.sqlmod.client.gui.PortableTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class WirelessTerminalItem extends Item {

    public WirelessTerminalItem(Properties properties) {
        super(properties.m_41487_(1));
    }

    @Override
    public InteractionResult m_6225_(UseOnContext context) {
        Level level = context.m_43725_();
        Player player = context.m_43723_();
        BlockPos pos = context.m_8083_();
        ItemStack stack = context.m_43722_();

        if (player != null && player.m_6144_()) {
            BlockEntity be = level.m_7702_(pos);
            if (be instanceof PortableTerminalBlockEntity) {
                if (!level.f_46443_) {
                    CompoundTag tag = stack.m_41784_();
                    tag.m_128365_("TerminalPos", NbtUtils.m_129224_(pos));
                    tag.m_128359_("TerminalDim", level.m_46472_().m_135782_().toString());

                    player.m_5661_(Component.m_237113_("§a[Terminal Remota] Enlazada a la Terminal en " + pos.m_123344_()), true);
                    if (level instanceof ServerLevel sl) {
                        sl.m_5594_(null, pos, SoundEvents.f_11704_, SoundSource.PLAYERS, 1.0F, 1.5F);
                    }
                }
                return InteractionResult.m_19078_(level.f_46443_);
            }
        }
        return super.m_6225_(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.m_21120_(hand);

        if (level.f_46443_) {
            return InteractionResultHolder.m_19098_(stack);
        }

        CompoundTag tag = stack.m_41783_();
        if (tag == null || !tag.m_128441_("TerminalPos")) {
            player.m_5661_(Component.m_237113_("§c[Terminal Remota] No está enlazada. Haz Shift + Clic Derecho sobre una Terminal Portable para enlazarla."), true);
            return InteractionResultHolder.m_19090_(stack);
        }

        BlockPos targetPos = NbtUtils.m_129239_(tag.m_128469_("TerminalPos"));
        String dimStr = tag.m_128461_("TerminalDim");

        ServerLevel targetLevel = (ServerLevel) level;
        if (!dimStr.isEmpty()) {
            for (ServerLevel sl : targetLevel.m_7654_().m_129785_()) {
                if (sl.m_46472_().m_135782_().toString().equals(dimStr)) {
                    targetLevel = sl;
                    break;
                }
            }
        }

        if (!targetLevel.m_46805_(targetPos)) {
            player.m_5661_(Component.m_237113_("§c[Terminal Remota] El chunk de la terminal objetivo no está cargado."), true);
            return InteractionResultHolder.m_19090_(stack);
        }

        BlockEntity be = targetLevel.m_7702_(targetPos);
        if (!(be instanceof PortableTerminalBlockEntity terminal)) {
            player.m_5661_(Component.m_237113_("§c[Terminal Remota] La terminal enlazada ya no existe en esas coordenadas."), true);
            return InteractionResultHolder.m_19090_(stack);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            SimpleMenuProvider provider = new SimpleMenuProvider(
                    (id, inv, p) -> new PortableTerminalMenu(id, inv, be),
                    Component.m_237113_("Wireless Portable Terminal")
            );
            NetworkHooks.openScreen(serverPlayer, provider, buf -> {
                buf.m_130064_(targetPos);
                buf.writeBoolean(terminal.getLinkedNetworkId() != null);
                BlockPos outPos = terminal.getOutputBoxPos();
                buf.writeBoolean(outPos != null);
                if (outPos != null) {
                    buf.m_130064_(outPos);
                    buf.m_130070_(terminal.getOutputBoxDimension());
                }
            });
            targetLevel.m_5594_(null, player.m_20183_(), SoundEvents.f_11704_, SoundSource.PLAYERS, 0.8F, 1.8F);
        }

        return InteractionResultHolder.m_19090_(stack);
    }

    @Override
    public void m_7373_(ItemStack stack, Level level, List<Component> tooltips, TooltipFlag flag) {
        CompoundTag tag = stack.m_41783_();
        if (tag != null && tag.m_128441_("TerminalPos")) {
            BlockPos pos = NbtUtils.m_129239_(tag.m_128469_("TerminalPos"));
            tooltips.add(Component.m_237113_("§aEnlazada a: §f" + pos.m_123344_()));
            tooltips.add(Component.m_237113_("§7Clic Derecho en aire: Abrir Terminal"));
            tooltips.add(Component.m_237113_("§8Shift+Clic Derecho en Terminal: Re-enlazar"));
        } else {
            tooltips.add(Component.m_237113_("§7Estado: §cDesconectada"));
            tooltips.add(Component.m_237113_("§eShift+Clic Derecho en una Terminal Portable para enlazar."));
        }
        super.m_7373_(stack, level, tooltips, flag);
    }
}
