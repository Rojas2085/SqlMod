package com.rojoh.sqlmod.block;

import com.rojoh.sqlmod.block.entity.PortableTerminalBlockEntity;
import com.rojoh.sqlmod.client.gui.PortableTerminalMenu;
import com.rojoh.sqlmod.init.ItemInit;
import com.rojoh.sqlmod.sql.SqlEngine;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class PortableTerminalBlock extends Block implements EntityBlock {
    public PortableTerminalBlock() {
        super(BlockBehaviour.Properties.m_284310_()
                .m_60978_(3.0F)
                .m_60918_(SoundType.f_56743_)
                .m_60955_());
    }

    @Override
    public BlockEntity m_142194_(BlockPos pos, BlockState state) {
        return new PortableTerminalBlockEntity(pos, state);
    }

    @Override
    public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.f_46443_) {
            return InteractionResult.m_19078_(true);
        }
        BlockEntity be = level.m_7702_(pos);
        if (!(be instanceof PortableTerminalBlockEntity terminal)) {
            return InteractionResult.PASS;
        }

        if (player.m_6144_() && player.m_21120_(hand).m_41619_() && terminal.getLinkedNetworkId() != null) {
            UUID netId = terminal.getLinkedNetworkId();
            terminal.unlinkNetworkKey();
            ItemStack keyStack = new ItemStack(ItemInit.NETWORK_KEY.get());
            keyStack.m_41784_().m_128362_("NetworkId", netId);
            if (!player.m_150109_().m_36054_(keyStack)) {
                player.m_36176_(keyStack, false);
            }
            player.m_5661_(Component.m_237113_("§eLlave de Red recuperada de la Terminal."), true);
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            SimpleMenuProvider provider = new SimpleMenuProvider(
                    (id, inv, p) -> new PortableTerminalMenu(id, inv, be),
                    Component.m_237113_("Portable Terminal")
            );
            NetworkHooks.openScreen(serverPlayer, provider, buf -> {
                buf.m_130064_(pos);
                buf.writeBoolean(terminal.getLinkedNetworkId() != null);
                BlockPos outPos = terminal.getOutputBoxPos();
                buf.writeBoolean(outPos != null);
                if (outPos != null) {
                    buf.m_130064_(outPos);
                    buf.m_130070_(terminal.getOutputBoxDimension());
                }
            });
        }
        return InteractionResult.m_19078_(false);
    }

    @Override
    public void m_6861_(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.m_6861_(state, level, pos, block, fromPos, isMoving);
        if (!level.m_5776_() && level.m_276867_(pos)) {
            BlockEntity be = level.m_7702_(pos);
            if (be instanceof PortableTerminalBlockEntity terminal) {
                String lastQuery = terminal.getLastQuery();
                if (lastQuery != null && !lastQuery.isBlank()) {
                    level.m_6907_().stream()
                            .filter(p -> p instanceof ServerPlayer)
                            .findFirst()
                            .ifPresent(p -> SqlEngine.executeQuery(level, pos, lastQuery, (ServerPlayer) p));
                }
            }
        }
    }

    @Override
    public void m_6402_(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (stack.m_41782_()) {
            BlockEntity be = level.m_7702_(pos);
            if (be instanceof PortableTerminalBlockEntity terminal) {
                CompoundTag tag = stack.m_41737_("BlockEntityTag");
                if (tag != null) {
                    terminal.m_142466_(tag);
                }
            }
        }
    }

    @Override
    public void m_5707_(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity be = level.m_7702_(pos);
        if (be instanceof PortableTerminalBlockEntity terminal && !level.f_46443_ && !player.m_7500_()) {
            ItemStack stack = new ItemStack(this);
            terminal.m_187476_(stack);
            ItemEntity itemEntity = new ItemEntity(level, pos.m_123341_() + 0.5D, pos.m_123342_() + 0.5D, pos.m_123343_() + 0.5D, stack);
            itemEntity.m_32060_();
            level.m_7967_(itemEntity);
        }
        super.m_5707_(level, pos, state, player);
    }
}
