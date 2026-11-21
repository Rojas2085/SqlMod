package com.rojoh.sqlmod.block.entity;

import com.rojoh.sqlmod.init.BlockEntityInit;
import com.rojoh.sqlmod.util.VfxHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkFirewallBlockEntity extends BlockEntity {

    private static final Set<NetworkFirewallBlockEntity> LOADED_FIREWALLS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static final double PROTECTION_RADIUS = 32.0D;

    private UUID ownerUUID;
    private String ownerName = "Desconocido";
    private boolean active = true;
    private final List<String> securityLog = new ArrayList<>();

    public NetworkFirewallBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.NETWORK_FIREWALL.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
            LOADED_FIREWALLS.add(this);
        }
    }

    @Override
    public void m_7651_() { // setRemoved
        super.m_7651_();
        LOADED_FIREWALLS.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        LOADED_FIREWALLS.remove(this);
    }

    public static boolean isBlocked(ServerLevel level, BlockPos targetPos, ServerPlayer player) {
        if (level == null || targetPos == null || player == null) return false;
        if (player.m_20310_(2)) return false; // Operadores no bloqueados

        for (NetworkFirewallBlockEntity fw : LOADED_FIREWALLS) {
            if (fw.f_58857_ == level && !fw.m_58901_() && fw.active && fw.ownerUUID != null) {
                if (fw.f_58858_.m_123314_(targetPos, PROTECTION_RADIUS)) {
                    if (!fw.ownerUUID.equals(player.m_20148_())) {
                        fw.logIntrusion(player.m_6302_());
                        VfxHelper.playTerminalError(level, targetPos);
                        VfxHelper.playTerminalError(level, fw.f_58858_);
                        player.m_5661_(Component.m_237113_("§c🛡 [FIREWALL] Acceso bloqueado por la red de §e" + fw.ownerName), true);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void logIntrusion(String playerName) {
        securityLog.add("DENY: " + playerName);
        if (securityLog.size() > 10) {
            securityLog.remove(0);
        }
        m_6596_();
    }

    public void setOwner(UUID uuid, String name) {
        this.ownerUUID = uuid;
        this.ownerName = name;
        m_6596_();
    }

    public boolean isOwner(UUID uuid) {
        return ownerUUID != null && ownerUUID.equals(uuid);
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isActive() {
        return active;
    }

    public void toggleActive() {
        this.active = !this.active;
        m_6596_();
    }

    public List<String> getSecurityLog() {
        return securityLog;
    }

    @Override
    protected void m_183515_(CompoundTag tag) {
        super.m_183515_(tag);
        if (ownerUUID != null) tag.m_128362_("OwnerUUID", ownerUUID);
        if (ownerName != null) tag.m_128359_("OwnerName", ownerName);
        tag.m_128379_("Active", active);
        ListTag logs = new ListTag();
        for (String s : securityLog) {
            logs.add(StringTag.m_129297_(s));
        }
        tag.m_128365_("SecurityLog", logs);
    }

    @Override
    public void m_142466_(CompoundTag tag) {
        super.m_142466_(tag);
        if (tag.m_128403_("OwnerUUID")) ownerUUID = tag.m_128342_("OwnerUUID");
        if (tag.m_128441_("OwnerName")) ownerName = tag.m_128461_("OwnerName");
        if (tag.m_128441_("Active")) active = tag.m_128471_("Active");
        securityLog.clear();
        if (tag.m_128441_("SecurityLog")) {
            ListTag logs = tag.m_128437_("SecurityLog", 8);
            for (int i = 0; i < logs.size(); i++) {
                securityLog.add(logs.m_128778_(i));
            }
        }
    }
}
