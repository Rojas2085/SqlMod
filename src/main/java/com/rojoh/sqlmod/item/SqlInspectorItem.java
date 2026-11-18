package com.rojoh.sqlmod.item;

import com.rojoh.sqlmod.sql.SqlEngine;
import com.rojoh.sqlmod.util.InventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import java.util.*;

public class SqlInspectorItem extends Item {

    public SqlInspectorItem(Properties properties) {
        super(properties.m_41487_(1));
    }

    @Override
    public InteractionResult m_6225_(UseOnContext context) {
        Level level = context.m_43725_();
        if (level.f_46443_) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.m_8083_();
        BlockEntity be = level.m_7702_(pos);
        Player player = context.m_43723_();
        if (player == null) return InteractionResult.PASS;

        LazyOptional<IItemHandler> invOpt = InventoryHelper.getInventory(level, pos, be, (net.minecraft.server.level.ServerPlayer) player);
        if (!invOpt.isPresent()) {
            player.m_5661_(Component.m_237113_("§c[Inspector SQL] Este bloque no tiene inventario compatible."), true);
            return InteractionResult.PASS;
        }

        IItemHandler handler = invOpt.orElseThrow(IllegalStateException::new);
        int totalSlots = handler.getSlots();
        int occupied = 0;
        int totalCount = 0;

        int countMinerals = 0;
        int countWeapons = 0;
        int countArmors = 0;
        int countTools = 0;
        int countBooks = 0;
        int countFood = 0;
        int countBlocks = 0;
        int countPotions = 0;
        int countEnchanted = 0;

        Map<String, Integer> topItems = new HashMap<>();

        for (int i = 0; i < totalSlots; i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.m_41619_()) {
                occupied++;
                int count = s.m_41613_();
                totalCount += count;

                String name = s.m_41786_().getString();
                topItems.put(name, topItems.getOrDefault(name, 0) + count);

                if (SqlEngine.isMineral(s)) countMinerals += count;
                if (SqlEngine.isWeapon(s)) countWeapons += count;
                if (SqlEngine.isArmor(s)) countArmors += count;
                if (SqlEngine.isTool(s)) countTools += count;
                if (SqlEngine.isBook(s)) countBooks += count;
                if (s.m_41720_().m_41472_()) countFood += count;
                if (SqlEngine.isBlock(s)) countBlocks += count;
                if (SqlEngine.isPotion(s)) countPotions += count;
                if (s.m_41793_()) countEnchanted += count;
            }
        }

        ServerLevel sl = (ServerLevel) level;
        sl.m_5594_(null, pos, SoundEvents.f_11704_, SoundSource.PLAYERS, 1.0F, 1.7F);
        sl.m_8767_(ParticleTypes.f_123796_, pos.m_123341_() + 0.5D, pos.m_123342_() + 1.1D, pos.m_123343_() + 0.5D, 20, 0.3D, 0.3D, 0.3D, 0.05D);

        player.m_213846_(Component.m_237113_("§b╔══════ [DIAGNÓSTICO SQL: " + pos.m_123344_() + "] ══════"));
        player.m_213846_(Component.m_237113_("§b║ §7Capacidad: §f" + occupied + "/" + totalSlots + " slots §7(§e" + totalCount + " ítems en total§7)"));
        player.m_213846_(Component.m_237113_("§b║ §eColumnas WHERE detectadas en este contenedor:"));

        if (countMinerals > 0) player.m_213846_(Component.m_237113_("§b║   • §aes_mineral = true §7(" + countMinerals + " ítems)"));
        if (countArmors > 0) player.m_213846_(Component.m_237113_("§b║   • §aes_armadura = true §7(" + countArmors + " ítems)"));
        if (countWeapons > 0) player.m_213846_(Component.m_237113_("§b║   • §aes_arma = true §7(" + countWeapons + " ítems)"));
        if (countTools > 0) player.m_213846_(Component.m_237113_("§b║   • §aes_herramienta = true §7(" + countTools + " ítems)"));
        if (countBooks > 0) player.m_213846_(Component.m_237113_("§b║   • §aes_libro = true §7(" + countBooks + " ítems)"));
        if (countFood > 0) player.m_213846_(Component.m_237113_("§b║   • §aes_comida = true §7(" + countFood + " ítems)"));
        if (countBlocks > 0) player.m_213846_(Component.m_237113_("§b║   • §aes_bloque = true §7(" + countBlocks + " ítems)"));
        if (countPotions > 0) player.m_213846_(Component.m_237113_("§b║   • §aes_pocion = true §7(" + countPotions + " ítems)"));
        if (countEnchanted > 0) player.m_213846_(Component.m_237113_("§b║   • §aencantado = true §7(" + countEnchanted + " ítems)"));

        player.m_213846_(Component.m_237113_("§b╚══════════════════════════════════════════════"));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void m_7373_(ItemStack stack, Level level, List<Component> tooltips, TooltipFlag flag) {
        tooltips.add(Component.m_237113_("§7Escáner de columnas y propiedades SQL."));
        tooltips.add(Component.m_237113_("§eClic Derecho en un cofre para ver sus variables WHERE."));
        super.m_7373_(stack, level, tooltips, flag);
    }
}
