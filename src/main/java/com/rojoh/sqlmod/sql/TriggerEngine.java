package com.rojoh.sqlmod.sql;

import com.rojoh.sqlmod.block.entity.SqlTerminalBlockEntity;
import com.rojoh.sqlmod.network.SqlModPacketHandler;
import com.rojoh.sqlmod.network.packet.QueryResponseS2CPacket;
import com.rojoh.sqlmod.util.InventoryHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;

public class TriggerEngine {

    public static void tick(Level level, BlockPos terminalPos, SqlTerminalBlockEntity terminal) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        List<SqlTrigger> triggers = terminal.getTriggers();
        if (triggers == null || triggers.isEmpty()) return;

        ServerPlayer executor = serverLevel.m_7654_().m_6846_().m_11314_().stream()
                .filter(p -> terminal.isOwner(p.m_20148_()))
                .findFirst()
                .orElse(serverLevel.m_7654_().m_6846_().m_11314_().stream().findFirst().orElse(null));

        Map<String, String> sourceOriginalName = new HashMap<>();
        Map<String, List<SqlTrigger>> bySource = new HashMap<>();

        for (SqlTrigger trigger : triggers) {
            if (!trigger.enabled) continue;
            String sourceKey = trigger.source.toLowerCase(Locale.ROOT);
            sourceOriginalName.putIfAbsent(sourceKey, trigger.source);
            bySource.computeIfAbsent(sourceKey, k -> new ArrayList<>()).add(trigger);
        }

        for (Map.Entry<String, List<SqlTrigger>> entry : bySource.entrySet()) {
            String sourceKey = entry.getKey();
            String sourceName = sourceOriginalName.get(sourceKey);
            BlockPos sourcePos = resolveSource(terminal, sourceName);

            if (sourcePos == null) {
                if (executor != null) {
                    notifyPlayer(executor, "§c[Trigger Engine] Error: Source chest '" + sourceName + "' for trigger is not linked or not found.");
                }
                continue;
            }

            BlockEntity be = level.m_7702_(sourcePos);
            if (be == null) {
                if (executor != null) {
                    notifyPlayer(executor, "§c[Trigger Engine] Error: Chest '" + sourceName + "' block entity is not loaded in the world.");
                }
                continue;
            }

            long currentHash = calculateInventoryHash(level, sourcePos, be, executor);
            long lastHash = terminal.getSourceHash(sourceKey);

            if (currentHash != lastHash) {
                terminal.setSourceHash(sourceKey, currentHash);

                for (SqlTrigger trigger : entry.getValue()) {
                    boolean matched = hasMatchingItems(level, sourcePos, be, executor, trigger.condition);
                    if (matched) {
                        notifyPlayer(executor, "§b[Trigger Engine] Trigger '" + trigger.name + "' matched condition: " + (trigger.condition != null ? trigger.condition : "*"));
                        String[] actions = trigger.action.split(";");
                        int fired = 0;
                        for (String rawAct : actions) {
                            String act = rawAct.trim();
                            if (act.isEmpty()) continue;

                            // Automatic expansion for INSERT INTO:
                            // If user configured "INSERT INTO <dest> WHERE <cond>" or "INSERT INTO <dest>"
                            // without an explicit SELECT, automatically extract from the trigger's source chest:
                            String actUpper = act.toUpperCase(Locale.ROOT);
                            if (actUpper.startsWith("INSERT INTO ") && !actUpper.contains(" SELECT ")) {
                                int whereIdx = actUpper.indexOf(" WHERE ");
                                if (whereIdx != -1) {
                                    String destChest = act.substring(12, whereIdx).trim();
                                    String wherePart = act.substring(whereIdx);
                                    act = "INSERT INTO " + destChest + " SELECT * FROM " + sourceName + " " + wherePart;
                                } else {
                                    String destChest = act.substring(12).trim();
                                    String condPart = (trigger.condition != null && !trigger.condition.isBlank() && !trigger.condition.equals("*"))
                                            ? " WHERE " + trigger.condition
                                            : "";
                                    act = "INSERT INTO " + destChest + " SELECT * FROM " + sourceName + condPart;
                                }
                            }

                            SqlEngine.executeQuery(level, terminalPos, act, executor);
                            fired++;
                        }
                        if (fired > 0) {
                            com.rojoh.sqlmod.util.VfxHelper.playTriggerFired(serverLevel, terminalPos);
                            if (executor != null) {
                                notifyPlayer(executor, "§b[Trigger §e" + trigger.name + "§b] §aFired §f" + fired + " action(s).");
                            }
                        }
                    } else {
                        if (executor != null) {
                            notifyPlayer(executor, "§7[Trigger Engine] Trigger '" + trigger.name + "' condition not met: " + trigger.condition);
                        }
                    }
                }
            }
        }
    }

    private static long calculateInventoryHash(Level level, BlockPos pos, BlockEntity be, ServerPlayer player) {
        long[] hash = new long[]{1L};
        InventoryHelper.getInventory(level, pos, be, player).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.m_41619_()) {
                    int itemId = Item.m_41393_(stack.m_41720_());
                    int count = stack.m_41613_();
                    hash[0] = hash[0] * 31L + (long) itemId * (long) count * (long) (i + 1);
                }
            }
        });
        return hash[0];
    }

    private static boolean hasMatchingItems(Level level, BlockPos pos, BlockEntity be, ServerPlayer player, String condition) {
        if (condition == null || condition.isBlank() || condition.equals("*")) return true;
        boolean[] match = new boolean[]{false};
        InventoryHelper.getInventory(level, pos, be, player).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.m_41619_() && SqlEngine.matchesWherePublic(stack, condition)) {
                    match[0] = true;
                    break;
                }
            }
        });
        return match[0];
    }

    private static BlockPos resolveSource(SqlTerminalBlockEntity terminal, String sourceName) {
        if (sourceName.equalsIgnoreCase("OutputBox") || sourceName.equalsIgnoreCase("output")) {
            return terminal.getOutputBoxPos();
        }
        return terminal.getLinkedChest(sourceName);
    }

    private static void notifyPlayer(ServerPlayer player, String message) {
        if (player == null) return;
        SqlModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new QueryResponseS2CPacket(message));
    }
}
