package com.rojoh.sqlmod.item;

import com.rojoh.sqlmod.block.entity.PortableTerminalBlockEntity;
import com.rojoh.sqlmod.block.entity.SqlTerminalBlockEntity;
import com.rojoh.sqlmod.sql.SqlEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.List;

public class QueryCardItem extends Item {

    public QueryCardItem(Properties properties) {
        super(properties.m_41487_(16));
    }

    public static String getStoredQuery(ItemStack stack) {
        CompoundTag tag = stack.m_41783_();
        if (tag != null && tag.m_128441_("StoredQuery")) {
            return tag.m_128461_("StoredQuery");
        }
        if (stack.m_41788_()) {
            return stack.m_41786_().getString();
        }
        return "";
    }

    @Override
    public InteractionResult m_6225_(UseOnContext context) {
        Level level = context.m_43725_();
        Player player = context.m_43723_();
        BlockPos pos = context.m_8083_();
        BlockEntity be = level.m_7702_(pos);
        ItemStack stack = context.m_43722_();

        boolean isTerminal = (be instanceof SqlTerminalBlockEntity) || (be instanceof PortableTerminalBlockEntity);
        if (!isTerminal) {
            return InteractionResult.PASS;
        }

        if (level.f_46443_) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel sl = (ServerLevel) level;

        // 1. Shift + Right Click: Save lastQuery from terminal onto card
        if (player != null && player.m_6144_()) {
            String lastQuery = null;
            if (be instanceof SqlTerminalBlockEntity te) lastQuery = te.getLastQuery();
            if (be instanceof PortableTerminalBlockEntity pe) lastQuery = pe.getLastQuery();

            if (lastQuery == null || lastQuery.isBlank()) {
                player.m_5661_(Component.m_237113_("§c[Tarjeta SQL] La terminal no tiene ninguna consulta previa ejecutada."), true);
                return InteractionResult.SUCCESS;
            }

            stack.m_41784_().m_128359_("StoredQuery", lastQuery);
            sl.m_5594_(null, pos, SoundEvents.f_11704_, SoundSource.PLAYERS, 1.0F, 1.4F);
            sl.m_8767_(ParticleTypes.f_123796_, pos.m_123341_() + 0.5D, pos.m_123342_() + 1.1D, pos.m_123343_() + 0.5D, 15, 0.2D, 0.2D, 0.2D, 0.05D);
            player.m_5661_(Component.m_237113_("§a[Tarjeta SQL] ¡Consulta grabada en la tarjeta!"), true);
            return InteractionResult.SUCCESS;
        }

        // 2. Normal Right Click: Execute stored query on terminal
        String query = getStoredQuery(stack);
        if (query.isBlank()) {
            if (player != null) {
                player.m_5661_(Component.m_237113_("§e[Tarjeta SQL] La tarjeta está vacía. Haz Shift+Clic Derecho en una terminal con una consulta para grabarla (o renómbrala en un yunque)."), true);
            }
            return InteractionResult.SUCCESS;
        }

        sl.m_5594_(null, pos, SoundEvents.f_12480_, SoundSource.PLAYERS, 1.0F, 1.2F);
        sl.m_8767_(ParticleTypes.f_123749_, pos.m_123341_() + 0.5D, pos.m_123342_() + 1.1D, pos.m_123343_() + 0.5D, 10, 0.2D, 0.2D, 0.2D, 0.05D);
        if (player instanceof ServerPlayer sp) {
            SqlEngine.executeQuery(level, pos, query, sp);
            player.m_5661_(Component.m_237113_("§a[Tarjeta SQL] Ejecutada consulta grabada."), true);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void m_7373_(ItemStack stack, Level level, List<Component> tooltips, TooltipFlag flag) {
        String query = getStoredQuery(stack);
        tooltips.add(Component.m_237113_("§7Tarjeta de Script SQL / Procedimiento Almacenado."));
        if (!query.isBlank()) {
            tooltips.add(Component.m_237113_("§eScript: §f" + (query.length() > 40 ? query.substring(0, 37) + "..." : query)));
            tooltips.add(Component.m_237113_("§aClic Derecho en Terminal: Ejecutar script"));
        } else {
            tooltips.add(Component.m_237113_("§8(Vacía - Shift+Clic Derecho en Terminal para grabar)"));
        }
        super.m_7373_(stack, level, tooltips, flag);
    }
}
