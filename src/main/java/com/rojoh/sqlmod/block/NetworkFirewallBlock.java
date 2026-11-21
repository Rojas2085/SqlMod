package com.rojoh.sqlmod.block;

import com.rojoh.sqlmod.block.entity.NetworkFirewallBlockEntity;
import com.rojoh.sqlmod.util.VfxHelper;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class NetworkFirewallBlock extends BaseEntityBlock {

    public NetworkFirewallBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape m_7514_(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity m_142194_(BlockPos pos, BlockState state) {
        return new NetworkFirewallBlockEntity(pos, state);
    }

    @Override
    public void m_6402_(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.m_6402_(level, pos, state, placer, stack);
        if (!level.f_46443_ && placer instanceof ServerPlayer sp) {
            BlockEntity be = level.m_7702_(pos);
            if (be instanceof NetworkFirewallBlockEntity fw) {
                fw.setOwner(sp.m_20148_(), sp.m_6302_());
                sp.m_5661_(Component.m_237113_("§a🛡 Firewall SQL activado (Radio: 32 bloques). Dueño: §e" + sp.m_6302_()), true);
            }
        }
    }

    @Override
    public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.m_5776_()) return InteractionResult.SUCCESS;

        BlockEntity be = level.m_7702_(pos);
        if (!(be instanceof NetworkFirewallBlockEntity fw)) return InteractionResult.PASS;

        if (!fw.isOwner(player.m_20148_()) && !player.m_20310_(2)) {
            fw.logIntrusion(player.m_6302_());
            if (level instanceof ServerLevel sl) VfxHelper.playTerminalError(sl, pos);
            player.m_5661_(Component.m_237113_("§c🛡 [FIREWALL] Solo el dueño (§e" + fw.getOwnerName() + "§c) puede configurar este Firewall."), true);
            return InteractionResult.FAIL;
        }

        if (player.m_6144_()) {
            List<String> logs = fw.getSecurityLog();
            player.m_213846_(Component.m_237113_("§6=== 🛡 FIREWALL SQL (" + (fw.isActive() ? "§aACTIVO" : "§cINACTIVO") + "§6) ==="));
            player.m_213846_(Component.m_237113_("§7Dueño: §f" + fw.getOwnerName() + " §7| Radio: §b32 bloques"));
            if (logs.isEmpty()) {
                player.m_213846_(Component.m_237113_("§aSin intentos de intrusión registrados."));
            } else {
                player.m_213846_(Component.m_237113_("§cÚltimos bloqueos (" + logs.size() + "): §f" + String.join(", ", logs)));
            }
        } else {
            fw.toggleActive();
            if (level instanceof ServerLevel sl) {
                if (fw.isActive()) VfxHelper.playTerminalSuccess(sl, pos);
                else VfxHelper.playTerminalError(sl, pos);
            }
            player.m_5661_(Component.m_237113_(fw.isActive()
                    ? "§a🛡 Firewall SQL: §lACTIVO §7(Protegiendo radio de 32 bloques)"
                    : "§e🛡 Firewall SQL: §lDESACTIVADO §7(Acceso abierto)"), true);
        }
        return InteractionResult.CONSUME;
    }
}
