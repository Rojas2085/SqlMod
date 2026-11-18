package com.rojoh.sqlmod.world.data;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class InfectionNetworkManager extends SavedData {
    private static final String DATA_NAME = "sqlmod_infection_networks";
    private final Map<UUID, InfectionNetwork> networks = new HashMap<>();

    public static class InfectionNetwork {
        public final UUID id;
        public final long expiresAt;
        public final List<BlockPos> chestPositions;

        public InfectionNetwork(UUID id, long expiresAt, List<BlockPos> chestPositions) {
            this.id = id;
            this.expiresAt = expiresAt;
            this.chestPositions = chestPositions;
        }
    }

    public InfectionNetworkManager() {}

    public static InfectionNetworkManager get(ServerLevel level) {
        ServerLevel overworld = level.m_7654_().m_129783_();
        DimensionDataStorage storage = overworld.m_8895_();
        return storage.m_164861_(InfectionNetworkManager::load, InfectionNetworkManager::new, DATA_NAME);
    }

    public static InfectionNetworkManager load(CompoundTag tag) {
        InfectionNetworkManager manager = new InfectionNetworkManager();
        ListTag list = tag.m_128437_("Networks", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag netTag = list.m_128728_(i);
            UUID id = netTag.m_128342_("Id");
            long expiresAt = netTag.m_128454_("ExpiresAt");
            List<BlockPos> chests = new ArrayList<>();
            ListTag chestList = netTag.m_128437_("Chests", 10);
            for (int j = 0; j < chestList.size(); j++) {
                chests.add(NbtUtils.m_129239_(chestList.m_128728_(j)));
            }
            manager.networks.put(id, new InfectionNetwork(id, expiresAt, chests));
        }
        return manager;
    }

    @Override
    public CompoundTag m_7176_(CompoundTag tag) {
        ListTag list = new ListTag();
        for (InfectionNetwork net : networks.values()) {
            CompoundTag netTag = new CompoundTag();
            netTag.m_128362_("Id", net.id);
            netTag.m_128356_("ExpiresAt", net.expiresAt);
            ListTag chestList = new ListTag();
            for (BlockPos pos : net.chestPositions) {
                chestList.add(NbtUtils.m_129224_(pos));
            }
            netTag.m_128365_("Chests", chestList);
            list.add(netTag);
        }
        tag.m_128365_("Networks", list);
        return tag;
    }

    public UUID createNetwork(long durationMs, List<BlockPos> chests) {
        UUID id = UUID.randomUUID();
        long expiresAt = System.currentTimeMillis() + durationMs;
        networks.put(id, new InfectionNetwork(id, expiresAt, chests));
        m_77762_();
        return id;
    }

    public InfectionNetwork getNetwork(UUID id) {
        InfectionNetwork net = networks.get(id);
        if (net != null) {
            if (System.currentTimeMillis() > net.expiresAt) {
                networks.remove(id);
                m_77762_();
                return null;
            }
            return net;
        }
        return null;
    }

    public Collection<InfectionNetwork> getAllNetworks() {
        return Collections.unmodifiableCollection(networks.values());
    }

    public InfectionNetwork findNetworkByChest(BlockPos pos) {
        for (InfectionNetwork net : networks.values()) {
            if (net.chestPositions.contains(pos)) {
                return net;
            }
        }
        return null;
    }

    public boolean removeChestFromNetwork(BlockPos pos) {
        boolean removed = false;
        for (InfectionNetwork net : networks.values()) {
            if (net.chestPositions.remove(pos)) {
                removed = true;
            }
        }
        if (removed) {
            m_77762_();
        }
        return removed;
    }

    public void tick() {
        long now = System.currentTimeMillis();
        boolean changed = networks.entrySet().removeIf(e -> e.getValue().expiresAt < now);
        if (changed) {
            m_77762_();
        }
    }
}
