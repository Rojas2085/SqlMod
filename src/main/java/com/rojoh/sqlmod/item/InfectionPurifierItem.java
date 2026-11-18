package com.rojoh.sqlmod.item;

import com.rojoh.sqlmod.util.InventoryHelper;
import com.rojoh.sqlmod.world.data.InfectionNetworkManager;
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

import java.util.List;

public class InfectionPurifierItem extends Item {

    public InfectionPurifierItem(Properties properties) {
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
        if (!InventoryHelper.hasInventory(level, pos, be)) {
            return InteractionResult.PASS;
        }

        Player player = context.m_43723_();
        if (player == null) return InteractionResult.PASS;

        ServerLevel serverLevel = (ServerLevel) level;
        ServerLevel overworld = serverLevel.m_7654_().m_129783_();
        InfectionNetworkManager manager = InfectionNetworkManager.get(overworld);

        boolean removed = manager.removeChestFromNetwork(pos);
        if (removed) {
            serverLevel.m_5594_(null, pos, SoundEvents.f_11904_, SoundSource.PLAYERS, 1.0F, 1.2F);
            serverLevel.m_8767_(ParticleTypes.f_123749_, pos.m_123341_() + 0.5D, pos.m_123342_() + 1.0D, pos.m_123343_() + 0.5D, 25, 0.3D, 0.3D, 0.3D, 0.05D);
            player.m_5661_(Component.m_237113_("§a[Purificador SQL] Cofre en " + pos.m_123344_() + " purificado y retirado de la red."), true);
        } else {
            player.m_5661_(Component.m_237113_("§7[Purificador SQL] Este cofre no pertenece a ninguna red infectada."), true);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void m_7373_(ItemStack stack, Level level, List<Component> tooltips, TooltipFlag flag) {
        tooltips.add(Component.m_237113_("§7Purifica cofres infectados."));
        tooltips.add(Component.m_237113_("§eClic Derecho en un cofre para desinfectarlo y retirarlo de la red."));
        tooltips.add(Component.m_237113_("§8No destruye el cofre ni afecta sus ítems."));
        super.m_7373_(stack, level, tooltips, flag);
    }
}
