package com.rojoh.sqlmod.item;

import com.rojoh.sqlmod.util.InventoryHelper;
import com.rojoh.sqlmod.world.data.InfectionNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

public class InfectionRelayItem extends Item {
    private static final int MAX_SEARCH_DEPTH_BLOCKS = 52;
    private static final int HOP_RADIUS = 12;
    private static final int MAX_ADD_CHESTS = 64;

    public InfectionRelayItem(Properties properties) {
        super(properties.m_41487_(1));
    }

    @Override
    public InteractionResult m_6225_(UseOnContext context) {
        Level level = context.m_43725_();
        if (level.f_46443_) {
            return InteractionResult.SUCCESS;
        }

        BlockPos originPos = context.m_8083_();
        BlockEntity originBe = level.m_7702_(originPos);
        if (!InventoryHelper.hasInventory(level, originPos, originBe)) {
            return InteractionResult.PASS;
        }

        Player player = context.m_43723_();
        if (player == null) return InteractionResult.PASS;

        ServerLevel serverLevel = (ServerLevel) level;
        ServerLevel overworld = serverLevel.m_7654_().m_129783_();
        InfectionNetworkManager manager = InfectionNetworkManager.get(overworld);
        ItemStack stack = context.m_43722_();
        CompoundTag tag = stack.m_41784_();

        // 1. Shift + Right Click: Select existing network from an infected chest
        if (player.m_6144_()) {
            InfectionNetworkManager.InfectionNetwork net = manager.findNetworkByChest(originPos);
            if (net != null) {
                tag.m_128362_("TargetNetworkId", net.id);
                serverLevel.m_5594_(null, originPos, SoundEvents.f_11704_, SoundSource.PLAYERS, 1.0F, 1.4F);
                serverLevel.m_8767_(ParticleTypes.f_123785_, originPos.m_123341_() + 0.5D, originPos.m_123342_() + 1.0D, originPos.m_123343_() + 0.5D, 15, 0.2D, 0.2D, 0.2D, 0.05D);
                player.m_5661_(Component.m_237113_("§d[Repetidor] Red seleccionada: " + net.id.toString().substring(0, 8) + "... Ahora haz Clic Derecho en un cofre nuevo para propagarla."), true);
                return InteractionResult.SUCCESS;
            } else {
                player.m_5661_(Component.m_237113_("§c[Repetidor] Este cofre no pertenece a ninguna red infectada."), true);
                return InteractionResult.SUCCESS;
            }
        }

        // 2. Normal Right Click: Expand into new chests
        if (!tag.m_128441_("TargetNetworkId")) {
            player.m_5661_(Component.m_237113_("§e[Repetidor] Primero haz Shift + Clic Derecho en un cofre infectado para seleccionar la red."), true);
            return InteractionResult.SUCCESS;
        }

        UUID netId = tag.m_128342_("TargetNetworkId");
        InfectionNetworkManager.InfectionNetwork targetNet = manager.getNetwork(netId);
        if (targetNet == null) {
            player.m_5661_(Component.m_237113_("§c[Repetidor] La red seleccionada ya expiró o no existe."), true);
            tag.m_128473_("TargetNetworkId");
            return InteractionResult.SUCCESS;
        }

        if (targetNet.chestPositions.contains(originPos)) {
            player.m_5661_(Component.m_237113_("§7[Repetidor] Este cofre ya está en la red seleccionada."), true);
            return InteractionResult.SUCCESS;
        }

        // Perform BFS search starting from this new chest
        Set<BlockPos> newlyInfected = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        newlyInfected.add(originPos);
        queue.add(originPos);

        while (!queue.isEmpty() && newlyInfected.size() < MAX_ADD_CHESTS) {
            BlockPos current = queue.poll();
            BlockPos.m_121990_(
                current.m_7918_(-HOP_RADIUS, -HOP_RADIUS, -HOP_RADIUS),
                current.m_7918_(HOP_RADIUS, HOP_RADIUS, HOP_RADIUS)
            ).forEach(pos -> {
                if (newlyInfected.size() >= MAX_ADD_CHESTS) return;
                if (pos.m_123333_(originPos) > MAX_SEARCH_DEPTH_BLOCKS) return;
                BlockPos immutable = pos.m_7949_();
                if (!newlyInfected.contains(immutable) && !targetNet.chestPositions.contains(immutable)) {
                    BlockEntity be = level.m_7702_(immutable);
                    if (be != null && InventoryHelper.hasInventory(level, immutable, be)) {
                        newlyInfected.add(immutable);
                        queue.add(immutable);
                    }
                }
            });
        }

        targetNet.chestPositions.addAll(newlyInfected);
        manager.m_77762_(); // mark dirty

        // Visual and sound effects
        serverLevel.m_5594_(null, originPos, SoundEvents.f_12616_, SoundSource.PLAYERS, 1.0F, 1.2F);
        for (BlockPos p : newlyInfected) {
            serverLevel.m_8767_(ParticleTypes.f_123785_, p.m_123341_() + 0.5D, p.m_123342_() + 1.0D, p.m_123343_() + 0.5D, 15, 0.2D, 0.2D, 0.2D, 0.05D);
            serverLevel.m_8767_(ParticleTypes.f_123745_, p.m_123341_() + 0.5D, p.m_123342_() + 0.5D, p.m_123343_() + 0.5D, 4, 0.2D, 0.2D, 0.2D, 0.02D);
        }

        player.m_5661_(Component.m_237113_("§d[Repetidor] ¡Red expandida! Se agregaron " + newlyInfected.size() + " cofres nuevos (Total en red: " + targetNet.chestPositions.size() + ")"), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void m_7373_(ItemStack stack, Level level, List<Component> tooltips, TooltipFlag flag) {
        CompoundTag tag = stack.m_41783_();
        if (tag != null && tag.m_128441_("TargetNetworkId")) {
            UUID id = tag.m_128342_("TargetNetworkId");
            tooltips.add(Component.m_237113_("§dRed Enlazada: §f" + id.toString().substring(0, 8) + "..."));
            tooltips.add(Component.m_237113_("§7Clic Derecho en nuevo cofre: Expandir red"));
            tooltips.add(Component.m_237113_("§8Shift+Clic Derecho en otro cofre infectado: Cambiar red"));
        } else {
            tooltips.add(Component.m_237113_("§7Sin red seleccionada."));
            tooltips.add(Component.m_237113_("§eShift+Clic Derecho en un cofre infectado para seleccionar la red."));
        }
        super.m_7373_(stack, level, tooltips, flag);
    }
}
