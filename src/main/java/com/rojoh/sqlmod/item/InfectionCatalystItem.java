package com.rojoh.sqlmod.item;

import com.rojoh.sqlmod.init.ItemInit;
import com.rojoh.sqlmod.util.InventoryHelper;
import com.rojoh.sqlmod.world.data.InfectionNetworkManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemHandlerHelper;

public class InfectionCatalystItem extends Item {
    // Increased from 32 to 52 (+20 blocks)
    private static final int MAX_SEARCH_DEPTH_BLOCKS = 52;
    private static final int HOP_RADIUS = 12;
    private static final int MAX_CHESTS = 128;

    public InfectionCatalystItem(Item.Properties pProperties) {
        super(pProperties);
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
        if (player == null) {
            return InteractionResult.PASS;
        }

        Set<BlockPos> infected = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        infected.add(originPos);
        queue.add(originPos);

        ServerLevel serverLevel = (ServerLevel) level;

        while (!queue.isEmpty() && infected.size() < MAX_CHESTS) {
            BlockPos current = queue.poll();
            BlockPos.m_121990_(
                current.m_7918_(-HOP_RADIUS, -HOP_RADIUS, -HOP_RADIUS),
                current.m_7918_(HOP_RADIUS, HOP_RADIUS, HOP_RADIUS)
            ).forEach(pos -> {
                if (infected.size() >= MAX_CHESTS) return;
                if (pos.m_123333_(originPos) > MAX_SEARCH_DEPTH_BLOCKS) return;
                BlockPos immutable = pos.m_7949_();
                if (!infected.contains(immutable)) {
                    BlockEntity be = level.m_7702_(immutable);
                    if (be != null && InventoryHelper.hasInventory(level, immutable, be)) {
                        infected.add(immutable);
                        queue.add(immutable);
                    }
                }
            });
        }

        InfectionNetworkManager manager = InfectionNetworkManager.get(serverLevel);
        UUID networkId = manager.createNetwork(serverLevel.m_46467_(), new ArrayList<>(infected));

        // Consume 1 catalyst
        context.m_43722_().m_41774_(1);

        // Give network key
        ItemStack keyStack = new ItemStack(ItemInit.NETWORK_KEY.get());
        keyStack.m_41784_().m_128362_("NetworkId", networkId);
        ItemHandlerHelper.giveItemToPlayer(player, keyStack);

        // Visual and sound effects
        serverLevel.m_5594_(null, originPos, SoundEvents.f_12616_, SoundSource.PLAYERS, 1.0F, 1.0F);
        for (BlockPos p : infected) {
            serverLevel.m_8767_(ParticleTypes.f_123785_, p.m_123341_() + 0.5D, p.m_123342_() + 1.0D, p.m_123343_() + 0.5D, 20, 0.3D, 0.3D, 0.3D, 0.05D);
            serverLevel.m_8767_(ParticleTypes.f_123745_, p.m_123341_() + 0.5D, p.m_123342_() + 0.5D, p.m_123343_() + 0.5D, 5, 0.3D, 0.3D, 0.3D, 0.02D);
        }

        player.m_5661_(Component.m_237113_("§d[Infección SQL] ¡Red creada con " + infected.size() + " cofres infectados! (Radio: " + MAX_SEARCH_DEPTH_BLOCKS + " bloques)"), true);
        return InteractionResult.SUCCESS;
    }
}
