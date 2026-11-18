package com.rojoh.sqlmod.item;

import com.rojoh.sqlmod.init.ItemInit;
import com.rojoh.sqlmod.world.data.InfectionNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public class NetworkVisorItem extends Item {

    public NetworkVisorItem(Properties properties) {
        super(properties.m_41487_(1));
    }

    private UUID resolvePlayerNetwork(Player player) {
        // 1. Offhand
        ItemStack offhand = player.m_21120_(net.minecraft.world.InteractionHand.OFF_HAND);
        if (offhand.m_41720_() == ItemInit.NETWORK_KEY.get()) {
            CompoundTag tag = offhand.m_41783_();
            if (tag != null && tag.m_128441_("NetworkId")) return tag.m_128342_("NetworkId");
        }
        if (offhand.m_41720_() instanceof NetworkKeyRingItem) {
            UUID key = NetworkKeyRingItem.getActiveKey(offhand);
            if (key != null) return key;
        }

        // 2. Inventory
        for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
            ItemStack s = player.m_150109_().m_8020_(i);
            if (s.m_41720_() == ItemInit.NETWORK_KEY.get()) {
                CompoundTag tag = s.m_41783_();
                if (tag != null && tag.m_128441_("NetworkId")) return tag.m_128342_("NetworkId");
            }
            if (s.m_41720_() instanceof NetworkKeyRingItem) {
                UUID key = NetworkKeyRingItem.getActiveKey(s);
                if (key != null) return key;
            }
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.m_21120_(hand);

        if (level.f_46443_) {
            return InteractionResultHolder.m_19098_(stack);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        ServerLevel overworld = serverLevel.m_7654_().m_129783_();
        InfectionNetworkManager manager = InfectionNetworkManager.get(overworld);

        UUID netId = resolvePlayerNetwork(player);
        if (netId == null) {
            player.m_5661_(Component.m_237113_("§c[Visor de Red] No tienes ninguna Llave de Red o Llavero en tu inventario."), true);
            return InteractionResultHolder.m_19090_(stack);
        }

        InfectionNetworkManager.InfectionNetwork net = manager.getNetwork(netId);
        if (net == null) {
            player.m_5661_(Component.m_237113_("§c[Visor de Red] La red ya expiró o no existe."), true);
            return InteractionResultHolder.m_19090_(stack);
        }

        int online = 0;
        int offline = 0;
        BlockPos playerPos = player.m_20183_();

        for (BlockPos cp : net.chestPositions) {
            boolean loaded = level.m_46805_(cp);
            if (loaded) {
                online++;
                if (cp.m_123333_(playerPos) <= 96) {
                    serverLevel.m_8767_(ParticleTypes.f_123749_, cp.m_123341_() + 0.5D, cp.m_123342_() + 1.2D, cp.m_123343_() + 0.5D, 8, 0.25D, 0.25D, 0.25D, 0.05D);
                    serverLevel.m_8767_(ParticleTypes.f_123785_, cp.m_123341_() + 0.5D, cp.m_123342_() + 0.5D, cp.m_123343_() + 0.5D, 6, 0.25D, 0.25D, 0.25D, 0.02D);
                }
            } else {
                offline++;
            }
        }

        serverLevel.m_5594_(null, player.m_20183_(), SoundEvents.f_11704_, SoundSource.PLAYERS, 0.9F, 1.8F);
        player.m_5661_(Component.m_237113_("§d[Visor de Red: " + netId.toString().substring(0, 8) + "] §aOnline: " + online + " §7| §cOffline: " + offline + " §7| §eTotal: " + net.chestPositions.size()), true);

        return InteractionResultHolder.m_19090_(stack);
    }

    @Override
    public void m_6883_(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        if (!level.f_46443_ && isSelected && level.m_46467_() % 40 == 0 && entity instanceof Player player) {
            UUID netId = resolvePlayerNetwork(player);
            if (netId != null) {
                ServerLevel sl = (ServerLevel) level;
                ServerLevel overworld = sl.m_7654_().m_129783_();
                InfectionNetworkManager.InfectionNetwork net = InfectionNetworkManager.get(overworld).getNetwork(netId);
                if (net != null) {
                    BlockPos pPos = player.m_20183_();
                    for (BlockPos cp : net.chestPositions) {
                        if (cp.m_123333_(pPos) <= 48 && level.m_46805_(cp)) {
                            sl.m_8767_(ParticleTypes.f_123749_, cp.m_123341_() + 0.5D, cp.m_123342_() + 1.1D, cp.m_123343_() + 0.5D, 2, 0.1D, 0.1D, 0.1D, 0.01D);
                        }
                    }
                }
            }
        }
        super.m_6883_(stack, level, entity, slot, isSelected);
    }

    @Override
    public void m_7373_(ItemStack stack, Level level, List<Component> tooltips, TooltipFlag flag) {
        tooltips.add(Component.m_237113_("§7Escáner de estado de la red infectada."));
        tooltips.add(Component.m_237113_("§eClic Derecho: Enviar pulso de sonar y contabilizar cofres"));
        tooltips.add(Component.m_237113_("§7Al sostenerlo en mano, resalta los cofres cercanos con partículas."));
        super.m_7373_(stack, level, tooltips, flag);
    }
}
