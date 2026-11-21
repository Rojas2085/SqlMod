package com.rojoh.sqlmod.block;

import com.rojoh.sqlmod.block.entity.AuditMailboxBlockEntity;
import com.rojoh.sqlmod.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AuditMailboxBlock extends BaseEntityBlock {

    public AuditMailboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape m_7514_(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity m_142194_(BlockPos pos, BlockState state) {
        return new AuditMailboxBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.m_5776_()) return null;
        return m_152132_(type, BlockEntityInit.AUDIT_MAILBOX.get(), (lvl, p, st, be) -> be.serverTick());
    }

    @Override
    public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.m_5776_()) return InteractionResult.SUCCESS;

        BlockEntity be = level.m_7702_(pos);
        if (!(be instanceof AuditMailboxBlockEntity mailbox)) return InteractionResult.PASS;

        if (player.m_6144_() && mailbox.hasPendingBatch()) {
            mailbox.flushBatchToBook();
        }

        ItemStack book = mailbox.popBook();
        if (!book.m_41619_()) {
            if (!player.m_150109_().m_36054_(book)) {
                player.m_36176_(book, false);
            }
            level.m_5594_(null, pos, SoundEvents.f_144243_, SoundSource.BLOCKS, 0.9F, 1.5F);
            player.m_5661_(Component.m_237113_("§a📖 Manifiesto SQL retirado. §7(Restantes: " + mailbox.getCompletedBookCount() + ")"), true);
        } else {
            String msg = mailbox.hasPendingBatch()
                    ? "§e📬 Lote en curso: §f" + mailbox.getPendingTotal() + " ítems §7(Shift+Clic para imprimir ya)"
                    : "§7📬 Buzón de Auditoría vacío. Colócalo a ≤24 bloques de una Terminal SQL.";
            player.m_5661_(Component.m_237113_(msg), true);
        }

        return InteractionResult.CONSUME;
    }
}
