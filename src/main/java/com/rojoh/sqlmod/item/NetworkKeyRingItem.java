package com.rojoh.sqlmod.item;

import com.rojoh.sqlmod.block.PortableTerminalBlock;
import com.rojoh.sqlmod.block.entity.PortableTerminalBlockEntity;
import com.rojoh.sqlmod.init.ItemInit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NetworkKeyRingItem extends Item {
    private static final int MAX_KEYS = 8;

    public NetworkKeyRingItem(Properties properties) {
        super(properties.m_41487_(1));
    }

    public static List<UUID> getStoredKeys(ItemStack stack) {
        List<UUID> list = new ArrayList<>();
        CompoundTag tag = stack.m_41783_();
        if (tag != null && tag.m_128441_("KeyRing")) {
            ListTag lTag = tag.m_128437_("KeyRing", 8);
            for (int i = 0; i < lTag.size(); i++) {
                try {
                    list.add(UUID.fromString(lTag.m_128778_(i)));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    public static int getActiveIndex(ItemStack stack) {
        CompoundTag tag = stack.m_41783_();
        if (tag != null) {
            return tag.m_128451_("ActiveIndex");
        }
        return 0;
    }

    public static UUID getActiveKey(ItemStack stack) {
        List<UUID> keys = getStoredKeys(stack);
        if (keys.isEmpty()) return null;
        int idx = getActiveIndex(stack);
        if (idx < 0 || idx >= keys.size()) idx = 0;
        return keys.get(idx);
    }

    @Override
    public InteractionResult m_6225_(UseOnContext context) {
        Level level = context.m_43725_();
        Player player = context.m_43723_();
        ItemStack stack = context.m_43722_();

        BlockEntity be = level.m_7702_(context.m_8083_());
        if (be instanceof PortableTerminalBlockEntity terminal) {
            if (level.f_46443_) {
                return InteractionResult.SUCCESS;
            }

            UUID activeKey = getActiveKey(stack);
            if (activeKey == null) {
                if (player != null) {
                    player.m_5661_(Component.m_237113_("§c[Llavero] El llavero está vacío. Coloca una Llave de Red en tu mano secundaria y haz clic derecho en el aire para guardarla."), true);
                }
                return InteractionResult.SUCCESS;
            }

            terminal.linkNetworkKey(activeKey);
            level.m_5594_(null, context.m_8083_(), SoundEvents.f_11704_, SoundSource.PLAYERS, 1.0F, 1.3F);
            if (player != null) {
                player.m_5661_(Component.m_237113_("§a[Llavero] ¡Terminal vinculada con la Red #" + (getActiveIndex(stack) + 1) + " (" + activeKey.toString().substring(0, 8) + ")!"), true);
            }
            return InteractionResult.SUCCESS;
        }

        return super.m_6225_(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.m_21120_(hand);
        ItemStack offhand = player.m_21120_(InteractionHand.OFF_HAND);

        // 1. Check if offhand has a NetworkKey to absorb
        if (offhand.m_41720_() == ItemInit.NETWORK_KEY.get()) {
            CompoundTag keyTag = offhand.m_41783_();
            if (keyTag != null && keyTag.m_128441_("NetworkId")) {
                UUID netId = keyTag.m_128342_("NetworkId");
                List<UUID> keys = getStoredKeys(stack);
                if (keys.size() >= MAX_KEYS) {
                    if (!level.f_46443_) {
                        player.m_5661_(Component.m_237113_("§c[Llavero] El llavero ya está lleno (máximo " + MAX_KEYS + " redes)."), true);
                    }
                    return InteractionResultHolder.m_19090_(stack);
                }

                if (keys.contains(netId)) {
                    if (!level.f_46443_) {
                        player.m_5661_(Component.m_237113_("§e[Llavero] Esta red ya está guardada en el llavero."), true);
                    }
                    return InteractionResultHolder.m_19090_(stack);
                }

                if (!level.f_46443_) {
                    CompoundTag tag = stack.m_41784_();
                    ListTag lTag = tag.m_128437_("KeyRing", 8);
                    lTag.add(StringTag.m_129297_(netId.toString()));
                    tag.m_128365_("KeyRing", lTag);
                    offhand.m_41774_(1);

                    level.m_5594_(null, player.m_20183_(), SoundEvents.f_11704_, SoundSource.PLAYERS, 1.0F, 1.5F);
                    player.m_5661_(Component.m_237113_("§a[Llavero] Llave agregada con éxito! (Total: " + (keys.size() + 1) + "/" + MAX_KEYS + ")"), true);
                }
                return InteractionResultHolder.m_19090_(stack);
            }
        }

        // 2. Shift + Right Click in air: Cycle active network
        if (player.m_6144_()) {
            List<UUID> keys = getStoredKeys(stack);
            if (keys.isEmpty()) {
                if (!level.f_46443_) {
                    player.m_5661_(Component.m_237113_("§7[Llavero] El llavero está vacío."), true);
                }
                return InteractionResultHolder.m_19090_(stack);
            }

            int nextIdx = (getActiveIndex(stack) + 1) % keys.size();
            stack.m_41784_().m_128405_("ActiveIndex", nextIdx);

            if (!level.f_46443_) {
                UUID active = keys.get(nextIdx);
                level.m_5594_(null, player.m_20183_(), SoundEvents.f_12480_, SoundSource.PLAYERS, 0.8F, 1.6F);
                player.m_5661_(Component.m_237113_("§b[Llavero] Red Activa: #" + (nextIdx + 1) + " (" + active.toString().substring(0, 8) + ")"), true);
            }
            return InteractionResultHolder.m_19090_(stack);
        }

        return super.m_7203_(level, player, hand);
    }

    @Override
    public void m_7373_(ItemStack stack, Level level, List<Component> tooltips, TooltipFlag flag) {
        List<UUID> keys = getStoredKeys(stack);
        int activeIdx = getActiveIndex(stack);

        tooltips.add(Component.m_237113_("§7Almacena hasta " + MAX_KEYS + " Llaves de Red."));
        if (keys.isEmpty()) {
            tooltips.add(Component.m_237113_("§8(Vacío - Coloca Llave de Red en mano secundaria y Clic Derecho)"));
        } else {
            for (int i = 0; i < keys.size(); i++) {
                String prefix = (i == activeIdx) ? "§a► " : "§7  ";
                String color = (i == activeIdx) ? "§a" : "§f";
                tooltips.add(Component.m_237113_(prefix + "Red #" + (i + 1) + ": " + color + keys.get(i).toString().substring(0, 8) + "..."));
            }
            tooltips.add(Component.m_237113_("§eShift+Clic Derecho en aire: Cambiar red activa"));
            tooltips.add(Component.m_237113_("§eClic Derecho en Terminal: Vincular red activa"));
        }
        super.m_7373_(stack, level, tooltips, flag);
    }
}
