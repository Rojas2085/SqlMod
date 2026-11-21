package com.rojoh.sqlmod.sql;

import com.rojoh.sqlmod.block.entity.PortableTerminalBlockEntity;
import com.rojoh.sqlmod.block.entity.SqlOutputBoxBlockEntity;
import com.rojoh.sqlmod.block.entity.SqlTerminalBlockEntity;
import com.rojoh.sqlmod.network.SqlModPacketHandler;
import com.rojoh.sqlmod.network.packet.QueryResponseS2CPacket;
import com.rojoh.sqlmod.network.packet.SyncTriggersS2CPacket;
import com.rojoh.sqlmod.util.InventoryHelper;
import com.rojoh.sqlmod.world.data.InfectionNetworkManager;
import com.rojoh.sqlmod.world.data.SqlScriptManager;
import com.rojoh.sqlmod.world.data.VirtualDeliverySystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

public class SqlEngine {

    public static class RowMatch {
        public BlockPos chestPos;
        public IItemHandler chestInv;
        public int slot;
        public ItemStack stack;

        public RowMatch(BlockPos chestPos, IItemHandler chestInv, int slot, ItemStack stack) {
            this.chestPos = chestPos;
            this.chestInv = chestInv;
            this.slot = slot;
            this.stack = stack;
        }
    }

    public static void executeQuery(Level level, BlockPos terminalPos, String query, ServerPlayer player) {
        if (level instanceof ServerLevel sLevel && com.rojoh.sqlmod.block.entity.NetworkFirewallBlockEntity.isBlocked(sLevel, terminalPos, player)) {
            sendResponse(player, "§c[FIREWALL] Acceso denegado por política de seguridad.");
            return;
        }
        BlockEntity be = level.m_7702_(terminalPos);
        if (be instanceof PortableTerminalBlockEntity portable) {
            executePortableQuery(level, portable, query, player);
            return;
        }
        if (!(be instanceof SqlTerminalBlockEntity terminal)) {
            sendResponse(player, "Error: Terminal not found.");
            return;
        }

        String raw = query == null ? "" : query.trim();
        if (raw.isEmpty()) return;

        String upperCheck = raw.toUpperCase(Locale.ROOT);
        if (raw.contains(";") && (!upperCheck.startsWith("CREATE MACRO ") || raw.toUpperCase(Locale.ROOT).contains("; CREATE "))) {
            String[] parts = raw.split("(?i);\\s*(?=CREATE\\s+|INSERT\\s+|SELECT\\s+|DELETE\\s+|DROP\\s+|ENABLE\\s+|DISABLE\\s+|OPTIMIZE\\s+|ALTER\\s+|DISTRIBUTE\\s+|RUN\\s+)");
            if (parts.length > 1) {
                for (String part : parts) {
                    String p = part.trim();
                    if (p.endsWith(";")) p = p.substring(0, p.length() - 1).trim();
                    if (!p.isEmpty()) {
                        executeSingleTerminalQuery(level, terminalPos, terminal, p, player);
                    }
                }
                return;
            } else if (!upperCheck.startsWith("CREATE TRIGGER ")) {
                String[] simpleParts = raw.split(";");
                if (simpleParts.length > 1) {
                    for (String part : simpleParts) {
                        String p = part.trim();
                        if (!p.isEmpty()) {
                            executeSingleTerminalQuery(level, terminalPos, terminal, p, player);
                        }
                    }
                    return;
                }
            }
        }

        if (raw.endsWith(";")) {
            raw = raw.substring(0, raw.length() - 1).trim();
        }
        executeSingleTerminalQuery(level, terminalPos, terminal, raw, player);
    }

    private static void executeSingleTerminalQuery(Level level, BlockPos terminalPos, SqlTerminalBlockEntity terminal, String cleanQuery, ServerPlayer player) {
        String upper = cleanQuery.toUpperCase(Locale.ROOT);

        if (upper.startsWith("CREATE MACRO ")) {
            executeMacroCreate(level, terminalPos, cleanQuery, player);
        } else if (upper.equals("SHOW MACROS")) {
            executeMacroShow(level, terminalPos, player);
        } else if (upper.startsWith("DROP MACRO ")) {
            executeMacroDrop(level, terminalPos, cleanQuery, player);
        } else if (upper.startsWith("EXEC ") || upper.startsWith("RUN ")) {
            executeMacroRun(level, terminalPos, cleanQuery, player);
        } else if (upper.startsWith("CREATE VIEW ")) {
            executeViewCreate(level, terminalPos, cleanQuery, player);
        } else if (upper.equals("SHOW VIEWS")) {
            executeViewShow(level, terminalPos, player);
        } else if (upper.startsWith("DROP VIEW ")) {
            executeViewDrop(level, terminalPos, cleanQuery, player);
        } else if (upper.equals("ROLLBACK")) {
            BlockPos outPos = terminal.getOutputBoxPos();
            if (outPos == null) {
                sendResponse(player, "Error: No Output Box linked for ROLLBACK.");
            } else {
                returnItems(level, outPos, player);
                sendResponse(player, "ROLLBACK ejecutado: ítems devueltos desde Output Box a sus cofres de origen.");
            }
        } else if (upper.startsWith("COMMIT TO ")) {
            String destChest = cleanQuery.substring(10).trim();
            executeInsert(level, terminal, "INSERT INTO " + destChest, player);
        } else if (upper.startsWith("OPTIMIZE TABLE ") || upper.startsWith("OPTIMIZE ")) {
            String target = upper.startsWith("OPTIMIZE TABLE ") ? cleanQuery.substring(15).trim() : cleanQuery.substring(9).trim();
            executeOptimize(level, terminal, target, player);
        } else if (upper.startsWith("ALTER TABLE ") || upper.startsWith("SORT TABLE ")) {
            executeSortTable(level, terminal, cleanQuery, upper, player);
        } else if (upper.startsWith("DISTRIBUTE FROM ")) {
            executeDistribute(level, terminal, cleanQuery, upper, player);
        } else if (upper.startsWith("SELECT")) {
            if (isAggregationQuery(upper)) {
                executeAggregateSelect(level, terminal, cleanQuery, upper, player);
            } else {
                executeSelect(level, terminal, cleanQuery, upper, player, null);
            }
        } else if (upper.startsWith("INSERT INTO")) {
            executeInsert(level, terminal, cleanQuery, player);
        } else if (upper.startsWith("DELETE FROM")) {
            executeDelete(level, terminal, cleanQuery, upper, player);
        } else if (upper.startsWith("CREATE TRIGGER")) {
            executeTriggerCreate(terminal, cleanQuery, player);
        } else if (upper.equals("SHOW TRIGGERS")) {
            executeTriggerShow(terminal, player);
        } else if (upper.startsWith("DROP TRIGGER")) {
            executeTriggerDrop(terminal, cleanQuery, player);
        } else if (upper.startsWith("ENABLE TRIGGER")) {
            executeTriggerSetEnabled(terminal, cleanQuery, player, true);
        } else if (upper.startsWith("DISABLE TRIGGER")) {
            executeTriggerSetEnabled(terminal, cleanQuery, player, false);
        } else {
            sendResponse(player, "Syntax Error. Try: SELECT, INSERT INTO, DELETE FROM, OPTIMIZE TABLE, ALTER TABLE, DISTRIBUTE, ROLLBACK, COMMIT TO, CREATE MACRO, CREATE TRIGGER");
        }
    }

    private static boolean isAggregationQuery(String upper) {
        return upper.startsWith("SELECT COUNT(") || upper.contains("SUM(") || upper.contains(" GROUP BY ");
    }

    private static void executeAggregateSelect(Level level, SqlTerminalBlockEntity terminal, String cleanQuery, String upper, ServerPlayer player) {
        int fromIdx = upper.indexOf(" FROM ");
        if (fromIdx == -1) {
            sendResponse(player, "Syntax Error: Missing FROM clause.");
            return;
        }
        String selectExpr = cleanQuery.substring(6, fromIdx).trim();
        int whereIdx = upper.indexOf(" WHERE ", fromIdx);
        int groupIdx = upper.indexOf(" GROUP BY ", fromIdx);
        int havingIdx = upper.indexOf(" HAVING ", fromIdx);
        int orderIdx = upper.indexOf(" ORDER BY ", fromIdx);

        int tableEnd = firstPositive(cleanQuery.length(), whereIdx, groupIdx, havingIdx, orderIdx);
        String sourceTable = cleanQuery.substring(fromIdx + 6, tableEnd).trim();

        String whereClause = null;
        if (whereIdx != -1) {
            int whereEnd = firstPositive(cleanQuery.length(), groupIdx, havingIdx, orderIdx);
            whereClause = cleanQuery.substring(whereIdx + 7, whereEnd).trim();
        }

        String groupByCol = null;
        if (groupIdx != -1) {
            int groupEnd = firstPositive(cleanQuery.length(), havingIdx, orderIdx);
            groupByCol = cleanQuery.substring(groupIdx + 10, groupEnd).trim().toLowerCase(Locale.ROOT);
        }

        String havingClause = null;
        if (havingIdx != -1) {
            int havingEnd = firstPositive(cleanQuery.length(), orderIdx);
            havingClause = cleanQuery.substring(havingIdx + 8, havingEnd).trim();
        }

        Map<String, BlockPos> chestsToScan = new LinkedHashMap<>();
        if (sourceTable.equals("*")) {
            chestsToScan.putAll(terminal.getLinkedChests());
        } else {
            BlockPos pos = terminal.getLinkedChest(sourceTable);
            if (pos == null) {
                sendResponse(player, "Error: Chest '" + sourceTable + "' not linked.");
                return;
            }
            chestsToScan.put(sourceTable, pos);
        }

        Map<String, Integer> groupedSums = new LinkedHashMap<>();
        int[] totalStacks = new int[]{0};
        int[] totalCount = new int[]{0};
        final String finalWhere = whereClause;
        final String finalGroupCol = groupByCol;

        for (Map.Entry<String, BlockPos> entry : chestsToScan.entrySet()) {
            String chestName = entry.getKey();
            BlockPos cp = entry.getValue();
            if (!level.m_46749_(cp)) continue;
            BlockEntity cbe = level.m_7702_(cp);
            if (cbe != null) {
                InventoryHelper.getInventory(level, cp, cbe, player).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.m_41619_() && matchesWhere(stack, finalWhere)) {
                            totalStacks[0]++;
                            totalCount[0] += stack.m_41613_();
                            String key = getGroupKey(stack, chestName, finalGroupCol != null ? finalGroupCol : "nombre");
                            groupedSums.put(key, groupedSums.getOrDefault(key, 0) + stack.m_41613_());
                        }
                    }
                });
            }
        }

        if (finalGroupCol == null && upper.startsWith("SELECT COUNT")) {
            sendResponse(player, "§aCOUNT(*): §e" + totalCount[0] + " ítems §7(" + totalStacks[0] + " stacks, " + groupedSums.size() + " tipos)");
            return;
        }
        if (finalGroupCol == null && upper.contains("SUM(")) {
            sendResponse(player, "§aSUM(cantidad): §e" + totalCount[0] + " ítems §7en " + chestsToScan.size() + " cofre(s).");
            return;
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(groupedSums.entrySet());
        if (havingClause != null && !havingClause.isEmpty()) {
            String hNorm = havingClause.replaceAll("(?i)SUM\\s*\\([^)]*\\)", "").replaceAll("(?i)COUNT\\s*\\([^)]*\\)", "").replaceAll("\\s+", "");
            list.removeIf(e -> !evaluateCondition(e.getValue(), hNorm));
        }

        boolean desc = orderIdx != -1 && upper.substring(orderIdx).contains("DESC");
        list.sort((a, b) -> desc ? Integer.compare(b.getValue(), a.getValue()) : Integer.compare(a.getValue(), b.getValue()));
        if (orderIdx == -1) {
            list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        }

        if (list.isEmpty()) {
            sendResponse(player, "§e0 grupos coinciden con la consulta.");
            return;
        }
        StringBuilder sb = new StringBuilder("§a=== GROUP BY " + finalGroupCol + " (" + list.size() + " grupos) ===");
        int shown = 0;
        for (Map.Entry<String, Integer> e : list) {
            sb.append("\n§b• ").append(e.getKey()).append(": §e").append(e.getValue());
            shown++;
            if (shown >= 15 && list.size() > 15) {
                sb.append("\n§7... y ").append(list.size() - 15).append(" grupos más.");
                break;
            }
        }
        sendResponse(player, sb.toString());
    }

    private static String getGroupKey(ItemStack stack, String chestName, String col) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        return switch (col) {
            case "mod" -> rl != null ? rl.m_135827_() : "unknown";
            case "rareza", "rarity" -> stack.m_41791_().name().toLowerCase(Locale.ROOT);
            case "cofre", "tabla", "chest", "table" -> chestName;
            default -> rl != null ? rl.m_135815_() : stack.m_41786_().getString();
        };
    }

    private static int firstPositive(int fallback, int... indices) {
        int min = fallback;
        for (int idx : indices) {
            if (idx != -1 && idx < min) min = idx;
        }
        return min;
    }

    private static void executeOptimize(Level level, SqlTerminalBlockEntity terminal, String target, ServerPlayer player) {
        List<BlockPos> targets = new ArrayList<>();
        if (target.equals("*")) {
            targets.addAll(terminal.getLinkedChests().values());
        } else {
            BlockPos p = terminal.getLinkedChest(target);
            if (p == null) {
                sendResponse(player, "Error: Chest '" + target + "' not linked.");
                return;
            }
            targets.add(p);
        }

        int[] mergedStacks = new int[]{0};
        int[] chestsOptimized = new int[]{0};
        for (BlockPos cp : targets) {
            if (!level.m_46749_(cp)) continue;
            BlockEntity cbe = level.m_7702_(cp);
            if (cbe != null) {
                InventoryHelper.getInventory(level, cp, cbe, player).ifPresent(handler -> {
                    int mergedHere = compactInventory(handler);
                    if (mergedHere >= 0) {
                        mergedStacks[0] += mergedHere;
                        chestsOptimized[0]++;
                    }
                });
            }
        }
        sendResponse(player, "§aOPTIMIZE completado en " + chestsOptimized[0] + " cofre(s): §e" + mergedStacks[0] + " stacks fusionados/compactados.");
    }

    private static int compactInventory(IItemHandler handler) {
        int merged = 0;
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack base = handler.getStackInSlot(i);
            if (base.m_41619_()) continue;
            int max = Math.min(base.m_41741_(), handler.getSlotLimit(i));
            if (base.m_41613_() >= max) continue;

            for (int j = i + 1; j < slots; j++) {
                ItemStack other = handler.getStackInSlot(j);
                if (other.m_41619_()) continue;
                if (ItemHandlerHelper.canItemStacksStack(base, other)) {
                    int space = max - handler.getStackInSlot(i).m_41613_();
                    if (space <= 0) break;
                    ItemStack simExt = handler.extractItem(j, space, true);
                    if (!simExt.m_41619_()) {
                        ItemStack simRem = handler.insertItem(i, simExt, true);
                        int accepted = simExt.m_41613_() - simRem.m_41613_();
                        if (accepted > 0) {
                            ItemStack realExt = handler.extractItem(j, accepted, false);
                            handler.insertItem(i, realExt, false);
                            merged++;
                        }
                    }
                }
            }
        }
        // Shift non-empty stacks left so empty slots are grouped at the end
        int writeIdx = 0;
        for (int readIdx = 0; readIdx < slots; readIdx++) {
            ItemStack current = handler.getStackInSlot(readIdx);
            if (!current.m_41619_()) {
                if (readIdx != writeIdx && handler.getStackInSlot(writeIdx).m_41619_()) {
                    ItemStack simExt = handler.extractItem(readIdx, current.m_41613_(), true);
                    ItemStack simRem = handler.insertItem(writeIdx, simExt, true);
                    if (simRem.m_41619_()) {
                        ItemStack realExt = handler.extractItem(readIdx, current.m_41613_(), false);
                        handler.insertItem(writeIdx, realExt, false);
                    }
                }
                writeIdx++;
            }
        }
        return merged;
    }

    private static void executeSortTable(Level level, SqlTerminalBlockEntity terminal, String cleanQuery, String upper, ServerPlayer player) {
        String target;
        String orderClause = "nombre ASC";
        if (upper.startsWith("ALTER TABLE ")) {
            int orderIdx = upper.indexOf(" ORDER BY ");
            if (orderIdx == -1) {
                sendResponse(player, "Syntax Error. Use: ALTER TABLE <cofre|*> ORDER BY <nombre|cantidad|dano|rareza|mod> [ASC|DESC]");
                return;
            }
            target = cleanQuery.substring(12, orderIdx).trim();
            orderClause = cleanQuery.substring(orderIdx + 10).trim();
        } else {
            int byIdx = upper.indexOf(" BY ");
            if (byIdx != -1) {
                target = cleanQuery.substring(11, byIdx).trim();
                orderClause = cleanQuery.substring(byIdx + 4).trim();
            } else {
                target = cleanQuery.substring(11).trim();
            }
        }

        List<BlockPos> targets = new ArrayList<>();
        if (target.equals("*")) {
            targets.addAll(terminal.getLinkedChests().values());
        } else {
            BlockPos p = terminal.getLinkedChest(target);
            if (p == null) {
                sendResponse(player, "Error: Chest '" + target + "' not linked.");
                return;
            }
            targets.add(p);
        }

        final String finalOrder = orderClause;
        int[] sortedChests = new int[]{0};
        for (BlockPos cp : targets) {
            if (!level.m_46749_(cp)) continue;
            BlockEntity cbe = level.m_7702_(cp);
            if (cbe != null) {
                InventoryHelper.getInventory(level, cp, cbe, player).ifPresent(handler -> {
                    compactInventory(handler);
                    List<RowMatch> tempRows = new ArrayList<>();
                    List<Integer> extractedSlots = new ArrayList<>();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack s = handler.getStackInSlot(i);
                        if (!s.m_41619_()) {
                            ItemStack sim = handler.extractItem(i, s.m_41613_(), true);
                            if (sim.m_41613_() == s.m_41613_()) {
                                extractedSlots.add(i);
                            }
                        }
                    }
                    for (int slot : extractedSlots) {
                        ItemStack ext = handler.extractItem(slot, handler.getStackInSlot(slot).m_41613_(), false);
                        if (!ext.m_41619_()) {
                            tempRows.add(new RowMatch(cp, handler, slot, ext));
                        }
                    }
                    applyOrder(tempRows, finalOrder);
                    for (RowMatch rm : tempRows) {
                        ItemHandlerHelper.insertItemStacked(handler, rm.stack, false);
                    }
                    sortedChests[0]++;
                });
            }
        }
        sendResponse(player, "§aOrdenamiento completado en " + sortedChests[0] + " cofre(s) [ORDER BY " + orderClause + "].");
    }

    private static void executeDistribute(Level level, SqlTerminalBlockEntity terminal, String cleanQuery, String upper, ServerPlayer player) {
        int intoIdx = upper.indexOf(" INTO ");
        if (intoIdx == -1) {
            sendResponse(player, "Syntax Error. Use: DISTRIBUTE FROM <origen|*> INTO (<dest1>, <dest2>) [WHERE ...] [LIMIT n]");
            return;
        }
        String sourceTable = cleanQuery.substring(16, intoIdx).trim();
        int openParen = cleanQuery.indexOf('(', intoIdx);
        int closeParen = cleanQuery.indexOf(')', openParen + 1);
        if (openParen == -1 || closeParen == -1) {
            sendResponse(player, "Syntax Error: Destination list must be in parentheses, e.g. INTO (cofre1, cofre2)");
            return;
        }
        String destListRaw = cleanQuery.substring(openParen + 1, closeParen).trim();
        String[] destNames = destListRaw.split(",");
        List<IItemHandler> destHandlers = new ArrayList<>();
        for (String dName : destNames) {
            String dn = dName.trim();
            if (dn.isEmpty()) continue;
            BlockPos dp = terminal.getLinkedChest(dn);
            if (dp == null) {
                sendResponse(player, "Error: Destination chest '" + dn + "' not linked.");
                return;
            }
            if (!level.m_46749_(dp)) continue;
            BlockEntity dbe = level.m_7702_(dp);
            if (dbe != null) {
                InventoryHelper.getInventory(level, dp, dbe, player).ifPresent(destHandlers::add);
            }
        }
        if (destHandlers.isEmpty()) {
            sendResponse(player, "Error: No valid loaded destination chests found.");
            return;
        }

        int whereIdx = upper.indexOf(" WHERE ", closeParen);
        int limitIdx = upper.indexOf(" LIMIT ", closeParen);
        String whereClause = null;
        if (whereIdx != -1) {
            int wEnd = limitIdx != -1 ? limitIdx : cleanQuery.length();
            whereClause = cleanQuery.substring(whereIdx + 7, wEnd).trim();
        }
        int limit = -1;
        if (limitIdx != -1) {
            try {
                limit = Integer.parseInt(cleanQuery.substring(limitIdx + 7).trim());
            } catch (NumberFormatException e) {
                sendResponse(player, "Error: LIMIT must be a number.");
                return;
            }
        }

        List<BlockPos> sources = new ArrayList<>();
        if (sourceTable.equals("*")) {
            sources.addAll(terminal.getLinkedChests().values());
        } else {
            BlockPos sp = terminal.getLinkedChest(sourceTable);
            if (sp == null) {
                sendResponse(player, "Error: Source chest '" + sourceTable + "' not linked.");
                return;
            }
            sources.add(sp);
        }

        List<RowMatch> matches = new ArrayList<>();
        final String fWhere = whereClause;
        for (BlockPos sp : sources) {
            if (!level.m_46749_(sp)) continue;
            BlockEntity sbe = level.m_7702_(sp);
            if (sbe != null) {
                InventoryHelper.getInventory(level, sp, sbe, player).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.m_41619_() && matchesWhere(stack, fWhere)) {
                            matches.add(new RowMatch(sp, handler, i, stack.m_41777_()));
                        }
                    }
                });
            }
        }

        List<RowMatch> finalMatches = (limit != -1 && limit < matches.size()) ? matches.subList(0, limit) : matches;

        int totalDistributed = 0;
        int rrIndex = 0;
        for (RowMatch rm : finalMatches) {
            ItemStack simExt = rm.chestInv.extractItem(rm.slot, rm.stack.m_41613_(), true);
            if (simExt.m_41619_()) continue;

            IItemHandler targetDest = destHandlers.get(rrIndex % destHandlers.size());
            rrIndex++;
            ItemStack simRem = ItemHandlerHelper.insertItemStacked(targetDest, simExt, true);
            int accepted = simExt.m_41613_() - simRem.m_41613_();
            if (accepted > 0) {
                ItemStack realExt = rm.chestInv.extractItem(rm.slot, accepted, false);
                ItemHandlerHelper.insertItemStacked(targetDest, realExt, false);
                totalDistributed += accepted;
            }
        }
        sendResponse(player, "§aDISTRIBUTE completado: §e" + totalDistributed + " ítems §arepartidos entre " + destHandlers.size() + " cofres.");
    }

    private static void executeMacroCreate(Level level, BlockPos pos, String cleanQuery, ServerPlayer player) {
        int asIdx = cleanQuery.toUpperCase(Locale.ROOT).indexOf(" AS ");
        if (asIdx == -1 || asIdx <= 13) {
            sendResponse(player, "Syntax Error. Use: CREATE MACRO <nombre> AS <comando1>; <comando2>");
            return;
        }
        String name = cleanQuery.substring(13, asIdx).trim().toLowerCase(Locale.ROOT);
        String body = cleanQuery.substring(asIdx + 4).trim();
        if (name.isEmpty() || body.isEmpty()) {
            sendResponse(player, "Syntax Error: Macro name and body cannot be empty.");
            return;
        }
        if (level instanceof ServerLevel sl) {
            SqlScriptManager.get(sl).putMacro(pos, name, body);
        }
        sendResponse(player, "§aMacro '" + name + "' guardada: §7" + body);
    }

    private static void executeMacroShow(Level level, BlockPos pos, ServerPlayer player) {
        Map<String, String> macros = level instanceof ServerLevel sl ? SqlScriptManager.get(sl).getMacros(pos) : Collections.emptyMap();
        if (macros.isEmpty()) {
            sendResponse(player, "§eNo hay macros guardadas en esta terminal.");
            return;
        }
        StringBuilder sb = new StringBuilder("§a=== Macros Registradas (" + macros.size() + ") ===");
        for (Map.Entry<String, String> e : macros.entrySet()) {
            sb.append("\n§b• ").append(e.getKey()).append(" §7-> ").append(e.getValue());
        }
        sendResponse(player, sb.toString());
    }

    private static void executeMacroDrop(Level level, BlockPos pos, String cleanQuery, ServerPlayer player) {
        String name = cleanQuery.substring(11).trim().toLowerCase(Locale.ROOT);
        boolean removed = level instanceof ServerLevel sl && SqlScriptManager.get(sl).removeMacro(pos, name);
        if (removed) {
            sendResponse(player, "§aMacro '" + name + "' eliminada.");
        } else {
            sendResponse(player, "§cMacro '" + name + "' no encontrada.");
        }
    }

    private static void executeMacroRun(Level level, BlockPos pos, String cleanQuery, ServerPlayer player) {
        int spaceIdx = cleanQuery.indexOf(' ');
        String name = cleanQuery.substring(spaceIdx + 1).trim().toLowerCase(Locale.ROOT);
        Map<String, String> macros = level instanceof ServerLevel sl ? SqlScriptManager.get(sl).getMacros(pos) : Collections.emptyMap();
        if (!macros.containsKey(name)) {
            sendResponse(player, "§cError: Macro '" + name + "' no existe. Usa SHOW MACROS.");
            return;
        }
        String body = macros.get(name);
        executeQuery(level, pos, body, player);
    }

    private static void executeViewCreate(Level level, BlockPos pos, String cleanQuery, ServerPlayer player) {
        int asIdx = cleanQuery.toUpperCase(Locale.ROOT).indexOf(" AS ");
        if (asIdx == -1 || asIdx <= 12) {
            sendResponse(player, "Syntax Error. Use: CREATE VIEW <nombre> AS SELECT * FROM <cofre> WHERE ...");
            return;
        }
        String name = cleanQuery.substring(12, asIdx).trim().toLowerCase(Locale.ROOT);
        String selectQuery = cleanQuery.substring(asIdx + 4).trim();
        if (!selectQuery.toUpperCase(Locale.ROOT).startsWith("SELECT")) {
            sendResponse(player, "Syntax Error: VIEW body must be a SELECT query.");
            return;
        }
        if (level instanceof ServerLevel sl) {
            SqlScriptManager.get(sl).putView(pos, name, selectQuery);
        }
        sendResponse(player, "§aVista '" + name + "' creada: §7" + selectQuery);
    }

    private static void executeViewShow(Level level, BlockPos pos, ServerPlayer player) {
        Map<String, String> views = level instanceof ServerLevel sl ? SqlScriptManager.get(sl).getViews(pos) : Collections.emptyMap();
        if (views.isEmpty()) {
            sendResponse(player, "§eNo hay vistas (VIEWS) creadas en esta terminal.");
            return;
        }
        StringBuilder sb = new StringBuilder("§a=== Vistas Virtuales (" + views.size() + ") ===");
        for (Map.Entry<String, String> e : views.entrySet()) {
            sb.append("\n§b• ").append(e.getKey()).append(" §7AS ").append(e.getValue());
        }
        sendResponse(player, sb.toString());
    }

    private static void executeViewDrop(Level level, BlockPos pos, String cleanQuery, ServerPlayer player) {
        String name = cleanQuery.substring(10).trim().toLowerCase(Locale.ROOT);
        boolean removed = level instanceof ServerLevel sl && SqlScriptManager.get(sl).removeView(pos, name);
        if (removed) {
            sendResponse(player, "§aVista '" + name + "' eliminada.");
        } else {
            sendResponse(player, "§cVista '" + name + "' no encontrada.");
        }
    }

    private static void executePortableQuery(Level level, PortableTerminalBlockEntity terminal, String query, ServerPlayer player) {
        String cleanQuery = query.trim();
        if (cleanQuery.endsWith(";")) {
            cleanQuery = cleanQuery.substring(0, cleanQuery.length() - 1).trim();
        }
        String upper = cleanQuery.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("SELECT")) {
            sendResponse(player, "Portable Terminal only supports SELECT queries.");
            return;
        }

        UUID netId = terminal.getLinkedNetworkId();
        if (netId == null) {
            sendResponse(player, "Error: No Network Key linked. Use Shift+RightClick with a key.");
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        InfectionNetworkManager.InfectionNetwork network = InfectionNetworkManager.get(serverLevel).getNetwork(netId);
        if (network == null) {
            sendResponse(player, "Error: Network expired or invalid.");
            return;
        }

        BlockPos outPos = terminal.getOutputBoxPos();
        String outDim = terminal.getOutputBoxDimension();
        if (outPos == null || outDim.isEmpty()) {
            sendResponse(player, "Error: No Output Box linked.");
            return;
        }

        int fromIdx = upper.indexOf(" FROM ");
        if (fromIdx == -1) {
            sendResponse(player, "Syntax Error: Missing FROM clause.");
            return;
        }

        String selector = cleanQuery.substring(7, fromIdx).trim();
        int whereIdx = upper.indexOf(" WHERE ", fromIdx);
        int orderIdx = upper.indexOf(" ORDER BY ", Math.max(fromIdx, whereIdx != -1 ? whereIdx : 0));
        int limitIdx = upper.indexOf(" LIMIT ", Math.max(fromIdx, Math.max(whereIdx != -1 ? whereIdx : 0, orderIdx != -1 ? orderIdx : 0)));

        String whereClause = null;
        if (whereIdx != -1) {
            int whereEnd = orderIdx != -1 ? orderIdx : (limitIdx != -1 ? limitIdx : cleanQuery.length());
            whereClause = cleanQuery.substring(whereIdx + 7, whereEnd).trim();
        }

        String orderClause = null;
        if (orderIdx != -1) {
            int orderEnd = limitIdx != -1 ? limitIdx : cleanQuery.length();
            orderClause = cleanQuery.substring(orderIdx + 10, orderEnd).trim();
        }

        int limit = -1;
        if (limitIdx != -1) {
            try {
                limit = Integer.parseInt(cleanQuery.substring(limitIdx + 7).trim());
            } catch (NumberFormatException e) {
                sendResponse(player, "Error: LIMIT must be a number.");
                return;
            }
        }

        List<RowMatch> matches = new ArrayList<>();
        final String finalWhere = whereClause;
        final String finalSelector = selector;

        for (BlockPos chestPos : network.chestPositions) {
            BlockEntity cbe = level.m_7702_(chestPos);
            if (cbe != null) {
                InventoryHelper.getInventory(level, chestPos, cbe, player).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.m_41619_() && matchesSelector(stack, finalSelector) && matchesWhere(stack, finalWhere)) {
                            matches.add(new RowMatch(chestPos, handler, i, stack.m_41777_()));
                        }
                    }
                });
            }
        }

        if (orderClause != null) {
            applyOrder(matches, orderClause);
        }

        List<RowMatch> finalMatches = matches;
        if (limit != -1 && limit < matches.size()) {
            finalMatches = matches.subList(0, limit);
        }

        VirtualDeliverySystem delivery = VirtualDeliverySystem.get(serverLevel);
        int totalExtracted = 0;
        for (RowMatch match : finalMatches) {
            ItemStack extracted = match.chestInv.extractItem(match.slot, match.stack.m_41613_(), false);
            extracted.m_41749_("SqlOriginX");
            extracted.m_41749_("SqlOriginY");
            extracted.m_41749_("SqlOriginZ");
            extracted.m_41749_("SqlOriginSlot");
            delivery.queueDelivery(outDim, outPos, extracted);
            totalExtracted += extracted.m_41613_();
        }

        if (totalExtracted > 0) {
            serverLevel.m_5594_(null, terminal.m_58899_(), SoundEvents.f_144243_, SoundSource.BLOCKS, 1.0F, 1.2F);
        }
        sendResponse(player, "Portable Query Success: " + totalExtracted + " items queued for delivery to Output Box.");
    }

    private static void executeInsert(Level level, SqlTerminalBlockEntity terminal, String cleanQuery, ServerPlayer player) {
        String upper = cleanQuery.toUpperCase(Locale.ROOT);
        int selectIdx = upper.indexOf(" SELECT ");
        if (selectIdx != -1) {
            if (cleanQuery.length() <= 12) {
                sendResponse(player, "Syntax Error. Try: INSERT INTO <chest> SELECT ...");
                return;
            }
            String destChest = cleanQuery.substring(12, selectIdx).trim();
            String selectPart = cleanQuery.substring(selectIdx + 1).trim();
            String selectUpper = selectPart.toUpperCase(Locale.ROOT);
            executeSelect(level, terminal, selectPart, selectUpper, player, destChest);
            return;
        }

        String[] tokens = cleanQuery.split("\\s+");
        if (tokens.length < 3) {
            sendResponse(player, "Syntax Error. Try: INSERT INTO <chest>");
            return;
        }
        String chestName = tokens[2];
        BlockPos chestPos = terminal.getLinkedChest(chestName);
        if (chestPos == null) {
            sendResponse(player, "Error: Chest '" + chestName + "' not linked.");
            return;
        }
        BlockPos outPos = terminal.getOutputBoxPos();
        if (outPos == null) {
            sendResponse(player, "Error: No Output Box linked.");
            return;
        }
        BlockEntity chestBe = level.m_7702_(chestPos);
        BlockEntity outBe = level.m_7702_(outPos);
        if (chestBe == null || !(outBe instanceof SqlOutputBoxBlockEntity outBox)) {
            sendResponse(player, "Error: Blocks are missing in the world.");
            return;
        }

        String whereClause = null;
        int whereIdx = upper.indexOf(" WHERE ");
        if (whereIdx != -1) {
            whereClause = cleanQuery.substring(whereIdx + 7).trim();
        }
        final String finalWhere = whereClause;
        final int batchSize = extractBatchSize(finalWhere, cleanQuery);

        InventoryHelper.getInventory(level, chestPos, chestBe, player).ifPresent(destHandler -> {
            ItemStackHandler outHandler = outBox.getItemHandler();
            int totalInserted = 0;
            Map<String, Integer> itemCounts = new LinkedHashMap<>();

            for (int i = 0; i < outHandler.getSlots(); i++) {
                ItemStack stack = outHandler.getStackInSlot(i);
                if (!stack.m_41619_() && matchesWhere(stack, finalWhere)) {
                    int toExtract = stack.m_41613_();
                    if (batchSize > 0) {
                        toExtract = (toExtract / batchSize) * batchSize;
                        if (toExtract <= 0) continue;
                    }
                    ItemStack extracted = outHandler.extractItem(i, toExtract, false);
                    extracted.m_41749_("SqlOriginX");
                    extracted.m_41749_("SqlOriginY");
                    extracted.m_41749_("SqlOriginZ");
                    extracted.m_41749_("SqlOriginSlot");
                    if (extracted.m_41783_() != null && extracted.m_41783_().m_128456_()) {
                        extracted.m_41751_(null);
                    }
                    ItemStack remainder = ItemHandlerHelper.insertItem(destHandler, extracted, false);
                    int inserted = extracted.m_41613_() - remainder.m_41613_();
                    if (inserted > 0) {
                        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(extracted.m_41720_());
                        String iName = rl != null ? rl.m_135815_() : extracted.m_41778_();
                        itemCounts.put(iName, itemCounts.getOrDefault(iName, 0) + inserted);
                        totalInserted += inserted;
                    }
                    if (!remainder.m_41619_()) {
                        outHandler.insertItem(i, remainder, false);
                    }
                }
            }

            StringBuilder summary = new StringBuilder();
            if (!itemCounts.isEmpty()) {
                summary.append(" [");
                int c = 0;
                for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                    if (c > 0) summary.append(", ");
                    summary.append(entry.getKey()).append("(").append(entry.getValue()).append(")");
                    c++;
                    if (c >= 5) {
                        summary.append(", ...");
                        break;
                    }
                }
                summary.append("]");
            }
            if (totalInserted > 0 && level instanceof ServerLevel sLevel) {
                sLevel.m_5594_(null, terminal.m_58899_(), SoundEvents.f_144243_, SoundSource.BLOCKS, 1.0F, 1.5F);
                sLevel.m_8767_(ParticleTypes.f_123760_, chestPos.m_123341_() + 0.5D, chestPos.m_123342_() + 1.0D, chestPos.m_123343_() + 0.5D, 20, 0.2D, 0.2D, 0.2D, 0.1D);
                sLevel.m_8767_(ParticleTypes.f_123809_, outPos.m_123341_() + 0.5D, outPos.m_123342_() + 1.0D, outPos.m_123343_() + 0.5D, 20, 0.2D, 0.2D, 0.2D, 0.1D);
            }
            sendResponse(player, "Inserted " + totalInserted + " items into '" + chestName + "'" + summary);
        });
    }

    private static void executeDelete(Level level, SqlTerminalBlockEntity terminal, String cleanQuery, String upper, ServerPlayer player) {
        int whereIdx = upper.indexOf(" WHERE ");
        int endTable = whereIdx != -1 ? whereIdx : cleanQuery.length();
        if (cleanQuery.length() <= 12) {
            sendResponse(player, "Syntax Error. Try: DELETE FROM <chest> [WHERE <condition>]");
            return;
        }
        String chestName = cleanQuery.substring(12, endTable).trim();
        BlockPos chestPos = terminal.getLinkedChest(chestName);
        if (chestPos == null) {
            sendResponse(player, "Error: Chest '" + chestName + "' not linked.");
            return;
        }
        BlockEntity chestBe = level.m_7702_(chestPos);
        if (chestBe == null) {
            sendResponse(player, "Error: Chest block entity not found in world.");
            return;
        }
        String whereClause = null;
        if (whereIdx != -1) {
            whereClause = cleanQuery.substring(whereIdx + 7).trim();
        }
        final String finalWhere = whereClause;

        InventoryHelper.getInventory(level, chestPos, chestBe, player).ifPresent(handler -> {
            int totalDeleted = 0;
            Map<String, Integer> itemCounts = new LinkedHashMap<>();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.m_41619_() && matchesWhere(stack, finalWhere)) {
                    ItemStack extracted = handler.extractItem(i, stack.m_41613_(), false);
                    int count = extracted.m_41613_();
                    if (count > 0) {
                        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(extracted.m_41720_());
                        String iName = rl != null ? rl.m_135815_() : extracted.m_41778_();
                        itemCounts.put(iName, itemCounts.getOrDefault(iName, 0) + count);
                        totalDeleted += count;
                    }
                }
            }
            StringBuilder summary = new StringBuilder();
            if (!itemCounts.isEmpty()) {
                summary.append(" [");
                int c = 0;
                for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                    if (c > 0) summary.append(", ");
                    summary.append(entry.getKey()).append("(").append(entry.getValue()).append(")");
                    c++;
                    if (c >= 5) {
                        summary.append(", ...");
                        break;
                    }
                }
                summary.append("]");
            }
            if (totalDeleted > 0 && level instanceof ServerLevel sLevel) {
                sLevel.m_5594_(null, terminal.m_58899_(), SoundEvents.f_12018_, SoundSource.BLOCKS, 1.0F, 1.0F);
                sLevel.m_8767_(ParticleTypes.f_123762_, chestPos.m_123341_() + 0.5D, chestPos.m_123342_() + 1.0D, chestPos.m_123343_() + 0.5D, 15, 0.2D, 0.2D, 0.2D, 0.0D);
            }
            sendResponse(player, "Deleted " + totalDeleted + " items from '" + chestName + "'" + summary);
        });
    }

    private static void executeSelect(Level level, SqlTerminalBlockEntity terminal, String cleanQuery, String upper, ServerPlayer player, String insertDestChest) {
        int fromIdx = upper.indexOf(" FROM ");
        if (fromIdx == -1) {
            sendResponse(player, "Syntax Error: Missing FROM clause.");
            return;
        }
        String selector = cleanQuery.substring(7, fromIdx).trim();
        int whereIdx = upper.indexOf(" WHERE ", fromIdx);
        int orderIdx = upper.indexOf(" ORDER BY ", Math.max(fromIdx, whereIdx != -1 ? whereIdx : 0));
        int limitIdx = upper.indexOf(" LIMIT ", Math.max(fromIdx, Math.max(whereIdx != -1 ? whereIdx : 0, orderIdx != -1 ? orderIdx : 0)));

        int tableEnd = whereIdx != -1 ? whereIdx : (orderIdx != -1 ? orderIdx : (limitIdx != -1 ? limitIdx : cleanQuery.length()));
        String sourceTable = cleanQuery.substring(fromIdx + 6, tableEnd).trim();

        // Check if sourceTable is a registered virtual VIEW
        Map<String, String> views = level instanceof ServerLevel sl ? SqlScriptManager.get(sl).getViews(terminal.m_58899_()) : Collections.emptyMap();
        if (views.containsKey(sourceTable.toLowerCase(Locale.ROOT))) {
            String viewSelect = views.get(sourceTable.toLowerCase(Locale.ROOT));
            executeSelect(level, terminal, viewSelect, viewSelect.toUpperCase(Locale.ROOT), player, insertDestChest);
            return;
        }

        String whereClause = null;
        if (whereIdx != -1) {
            int whereEnd = orderIdx != -1 ? orderIdx : (limitIdx != -1 ? limitIdx : cleanQuery.length());
            whereClause = cleanQuery.substring(whereIdx + 7, whereEnd).trim();
        }

        String orderClause = null;
        if (orderIdx != -1) {
            int orderEnd = limitIdx != -1 ? limitIdx : cleanQuery.length();
            orderClause = cleanQuery.substring(orderIdx + 10, orderEnd).trim();
        }

        int limit = -1;
        if (limitIdx != -1) {
            try {
                limit = Integer.parseInt(cleanQuery.substring(limitIdx + 7).trim());
            } catch (NumberFormatException e) {
                sendResponse(player, "Error: LIMIT must be a number.");
                return;
            }
        }

        List<BlockPos> sourcePositions = new ArrayList<>();
        if (sourceTable.equals("*")) {
            sourcePositions.addAll(terminal.getLinkedChests().values());
            if (sourcePositions.isEmpty()) {
                sendResponse(player, "Error: No chests linked to terminal.");
                return;
            }
        } else {
            BlockPos pos = (sourceTable.equalsIgnoreCase("OutputBox") || sourceTable.equalsIgnoreCase("output"))
                    ? terminal.getOutputBoxPos()
                    : terminal.getLinkedChest(sourceTable);
            if (pos == null) {
                sendResponse(player, "Error: Chest '" + sourceTable + "' not linked.");
                return;
            }
            sourcePositions.add(pos);
        }

        IItemHandler destHandler;
        BlockPos destPos;
        if (insertDestChest == null) {
            BlockPos outPos = terminal.getOutputBoxPos();
            if (outPos == null) {
                sendResponse(player, "Error: No Output Box linked.");
                return;
            }
            BlockEntity outBe = level.m_7702_(outPos);
            if (!(outBe instanceof SqlOutputBoxBlockEntity outBox)) {
                sendResponse(player, "Error: Output Box missing in world.");
                return;
            }
            destHandler = outBox.getItemHandler();
            destPos = outPos;
        } else {
            BlockPos targetPos = (insertDestChest.equalsIgnoreCase("OutputBox") || insertDestChest.equalsIgnoreCase("output"))
                    ? terminal.getOutputBoxPos()
                    : terminal.getLinkedChest(insertDestChest);
            if (targetPos == null) {
                sendResponse(player, "Error: Target chest '" + insertDestChest + "' not linked.");
                return;
            }
            BlockEntity targetBe = level.m_7702_(targetPos);
            if (targetBe == null) {
                sendResponse(player, "Error: Target chest block entity not found in world.");
                return;
            }
            LazyOptional<IItemHandler> cap = InventoryHelper.getInventory(level, targetPos, targetBe, player);
            if (!cap.isPresent()) {
                sendResponse(player, "Error: Target chest has no inventory capability.");
                return;
            }
            destHandler = cap.orElseThrow(IllegalStateException::new);
            destPos = targetPos;
        }

        final String finalWhere = whereClause;
        final String finalSelector = selector;
        List<RowMatch> matches = new ArrayList<>();

        for (BlockPos cp : sourcePositions) {
            BlockEntity cbe = level.m_7702_(cp);
            if (cbe != null) {
                InventoryHelper.getInventory(level, cp, cbe, player).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.m_41619_() && matchesSelector(stack, finalSelector) && matchesWhere(stack, finalWhere)) {
                            matches.add(new RowMatch(cp, handler, i, stack.m_41777_()));
                        }
                    }
                });
            }
        }

        if (orderClause != null) {
            applyOrder(matches, orderClause);
        }

        List<RowMatch> finalMatches = matches;
        if (limit != -1 && limit < matches.size()) {
            finalMatches = matches.subList(0, limit);
        }

        int totalMoved = 0;
        int overflowMoved = 0;
        Map<String, Integer> itemCounts = new LinkedHashMap<>();
        int batchSize = extractBatchSize(finalWhere, cleanQuery);
        Set<BlockPos> affectedOrigins = new HashSet<>();
        Set<BlockPos> overflowOrigins = new HashSet<>();

        // Resolve optional Overflow / Desborde chest automatically (no SQL command changes)
        BlockPos overflowPos = null;
        IItemHandler overflowHandler = null;
        if (insertDestChest != null) {
            for (Map.Entry<String, BlockPos> ovEntry : terminal.getLinkedChests().entrySet()) {
                String k = ovEntry.getKey();
                if (k.equalsIgnoreCase("overflow") || k.equalsIgnoreCase("desborde")) {
                    BlockPos candPos = ovEntry.getValue();
                    if (!candPos.equals(destPos) && level.m_46749_(candPos)) {
                        BlockEntity ovBe = level.m_7702_(candPos);
                        if (ovBe != null) {
                            LazyOptional<IItemHandler> ovCap = InventoryHelper.getInventory(level, candPos, ovBe, player);
                            if (ovCap.isPresent()) {
                                overflowPos = candPos;
                                overflowHandler = ovCap.orElseThrow(IllegalStateException::new);
                                break;
                            }
                        }
                    }
                }
            }
        }

        for (RowMatch match : finalMatches) {
            int toExtract = match.stack.m_41613_();
            if (batchSize > 0) {
                toExtract = (toExtract / batchSize) * batchSize;
                if (toExtract <= 0) continue;
            }

            // 1. Simulated extraction & destination insertion (Zero-Loss / Zero-Duplication atomicity)
            ItemStack simExtracted = match.chestInv.extractItem(match.slot, toExtract, true);
            if (simExtracted.m_41619_()) continue;

            ItemStack simForDest = simExtracted.m_41777_();
            if (insertDestChest == null) {
                CompoundTag tag = simForDest.m_41784_();
                tag.m_128405_("SqlOriginX", match.chestPos.m_123341_());
                tag.m_128405_("SqlOriginY", match.chestPos.m_123342_());
                tag.m_128405_("SqlOriginZ", match.chestPos.m_123343_());
                tag.m_128405_("SqlOriginSlot", match.slot);
            } else {
                simForDest.m_41749_("SqlOriginX");
                simForDest.m_41749_("SqlOriginY");
                simForDest.m_41749_("SqlOriginZ");
                simForDest.m_41749_("SqlOriginSlot");
                if (simForDest.m_41783_() != null && simForDest.m_41783_().m_128456_()) {
                    simForDest.m_41751_(null);
                }
            }

            ItemStack simRemDest = ItemHandlerHelper.insertItemStacked(destHandler, simForDest, true);
            int acceptedDest = simForDest.m_41613_() - simRemDest.m_41613_();

            int acceptedOverflow = 0;
            if (!simRemDest.m_41619_() && overflowHandler != null) {
                ItemStack simRemOv = ItemHandlerHelper.insertItemStacked(overflowHandler, simRemDest.m_41777_(), true);
                acceptedOverflow = simRemDest.m_41613_() - simRemOv.m_41613_();
            }

            int totalAccepted = acceptedDest + acceptedOverflow;
            if (totalAccepted <= 0) continue;

            // 2. Real extraction of ONLY the verified accepted amount
            ItemStack extracted = match.chestInv.extractItem(match.slot, totalAccepted, false);
            if (extracted.m_41619_()) continue;

            if (insertDestChest == null) {
                CompoundTag tag = extracted.m_41784_();
                tag.m_128405_("SqlOriginX", match.chestPos.m_123341_());
                tag.m_128405_("SqlOriginY", match.chestPos.m_123342_());
                tag.m_128405_("SqlOriginZ", match.chestPos.m_123343_());
                tag.m_128405_("SqlOriginSlot", match.slot);
            } else {
                extracted.m_41749_("SqlOriginX");
                extracted.m_41749_("SqlOriginY");
                extracted.m_41749_("SqlOriginZ");
                extracted.m_41749_("SqlOriginSlot");
                if (extracted.m_41783_() != null && extracted.m_41783_().m_128456_()) {
                    extracted.m_41751_(null);
                }
            }

            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(extracted.m_41720_());
            String iName = rl != null ? rl.m_135815_() : extracted.m_41778_();

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(destHandler, extracted, false);
            int movedToDest = extracted.m_41613_() - remainder.m_41613_();
            if (movedToDest > 0) {
                itemCounts.put(iName, itemCounts.getOrDefault(iName, 0) + movedToDest);
                totalMoved += movedToDest;
                affectedOrigins.add(match.chestPos);
            }

            // Route remainder to Overflow chest if available
            if (!remainder.m_41619_() && overflowHandler != null) {
                int beforeOv = remainder.m_41613_();
                remainder = ItemHandlerHelper.insertItemStacked(overflowHandler, remainder, false);
                int movedOv = beforeOv - remainder.m_41613_();
                if (movedOv > 0) {
                    itemCounts.put(iName, itemCounts.getOrDefault(iName, 0) + movedOv);
                    totalMoved += movedOv;
                    overflowMoved += movedOv;
                    overflowOrigins.add(match.chestPos);
                }
            }

            // Final safety rollback if anything still remained
            if (!remainder.m_41619_()) {
                if (insertDestChest == null) {
                    remainder.m_41749_("SqlOriginX");
                    remainder.m_41749_("SqlOriginY");
                    remainder.m_41749_("SqlOriginZ");
                    remainder.m_41749_("SqlOriginSlot");
                    if (remainder.m_41783_() != null && remainder.m_41783_().m_128456_()) {
                        remainder.m_41751_(null);
                    }
                }
                ItemHandlerHelper.insertItemStacked(match.chestInv, remainder, false);
            }
        }

        if (totalMoved > 0 && level instanceof ServerLevel sLevel) {
            for (BlockPos origPos : affectedOrigins) {
                com.rojoh.sqlmod.util.VfxHelper.spawnTransferBeam(sLevel, origPos, destPos);
            }
            if (overflowPos != null) {
                for (BlockPos ovOrig : overflowOrigins) {
                    com.rojoh.sqlmod.util.VfxHelper.spawnTransferBeam(sLevel, ovOrig, overflowPos);
                }
            }
            com.rojoh.sqlmod.util.VfxHelper.playTerminalSuccess(sLevel, terminal.m_58899_());
            com.rojoh.sqlmod.block.entity.AuditMailboxBlockEntity.recordTransfer(sLevel, terminal.m_58899_(), insertDestChest, itemCounts, overflowMoved);
        }

        StringBuilder summary = new StringBuilder();
        if (!itemCounts.isEmpty()) {
            summary.append(" [");
            int c = 0;
            for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                if (c > 0) summary.append(", ");
                summary.append(entry.getKey()).append("(").append(entry.getValue()).append(")");
                c++;
                if (c >= 5) {
                    summary.append(", ...");
                    break;
                }
            }
            summary.append("]");
        }
        String targetLabel = insertDestChest == null ? "Output Box" : insertDestChest;
        String ovNote = overflowMoved > 0 ? " §6[OVERFLOW: " + overflowMoved + " desviados]" : "";
        sendResponse(player, "Moved " + totalMoved + " items to " + targetLabel + summary + ovNote + " (" + finalMatches.size() + " stacks matched).");
    }

    private static boolean matchesSelector(ItemStack stack, String selector) {
        if (selector.equals("*")) return true;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        String sLow = selector.toLowerCase(Locale.ROOT);
        return rl != null && (rl.m_135815_().contains(sLow) || rl.toString().contains(sLow));
    }

    public static boolean matchesWherePublic(ItemStack stack, String whereClause) {
        return matchesWhere(stack, whereClause);
    }

    private static boolean matchesWhere(ItemStack stack, String whereClause) {
        if (whereClause == null || whereClause.isBlank()) return true;
        return evalBooleanExpr(stack, whereClause.trim());
    }

    private static boolean evalBooleanExpr(ItemStack stack, String expr) {
        expr = expr.trim();
        if (expr.isEmpty()) return true;

        while (expr.startsWith("(") && expr.endsWith(")") && isOuterParentheses(expr)) {
            expr = expr.substring(1, expr.length() - 1).trim();
        }

        // 1. Top-level OR (lowest precedence in boolean logic)
        int orIdx = findTopLevelOp(expr, "OR");
        if (orIdx != -1) {
            String left = expr.substring(0, orIdx).trim();
            String right = expr.substring(orIdx + 2).trim();
            return evalBooleanExpr(stack, left) || evalBooleanExpr(stack, right);
        }

        // 2. Top-level AND
        int andIdx = findTopLevelOp(expr, "AND");
        if (andIdx != -1) {
            String left = expr.substring(0, andIdx).trim();
            String right = expr.substring(andIdx + 3).trim();
            return evalBooleanExpr(stack, left) && evalBooleanExpr(stack, right);
        }

        // 3. Prefix NOT
        if (expr.toUpperCase(Locale.ROOT).startsWith("NOT ") && expr.length() > 4) {
            return !evalBooleanExpr(stack, expr.substring(4).trim());
        }

        return matchesSingleCondition(stack, expr);
    }

    private static int findTopLevelOp(String expr, String op) {
        int depth = 0;
        int opLen = op.length();
        int n = expr.length();
        for (int i = 0; i <= n - opLen; i++) {
            char c = expr.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (depth == 0) {
                if (expr.regionMatches(true, i, op, 0, opLen)) {
                    boolean startBoundary = (i == 0 || Character.isWhitespace(expr.charAt(i - 1)));
                    boolean endBoundary = (i + opLen == n || Character.isWhitespace(expr.charAt(i + opLen)));
                    if (startBoundary && endBoundary) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private static boolean isOuterParentheses(String expr) {
        int depth = 0;
        for (int i = 0; i < expr.length() - 1; i++) {
            char c = expr.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            if (depth == 0) return false;
        }
        return true;
    }

    private static boolean matchesSingleCondition(ItemStack stack, String cond) {
        String cleanCond = cond.trim();

        // NOT LIKE handling: e.g. "nombre NOT LIKE '%netherite%'"
        if (cleanCond.toUpperCase(Locale.ROOT).contains(" NOT LIKE ")) {
            String pos = cleanCond.replaceFirst("(?i)\\s+NOT\\s+LIKE\\s+", " LIKE ");
            return !matchesSingleCondition(stack, pos);
        }

        // Tolerance for missing LIKE keyword: e.g. "nombre '%arrow%'" or "mineral '%gem%'"
        if (!cleanCond.toUpperCase(Locale.ROOT).contains(" LIKE ") && !cleanCond.contains("=") && (cleanCond.contains("'") || cleanCond.contains("\""))) {
            cleanCond = cleanCond.replaceFirst("(?i)^([a-zA-Z_0-9]+)\\s+(['\"].*['\"])$", "$1 LIKE $2");
        }

        String noSpace = cleanCond.replaceAll("\\s+", "");
        String lowNoSpace = noSpace.toLowerCase(Locale.ROOT);

        // Numeric conditions: count / cantidad
        if (lowNoSpace.startsWith("count")) {
            return evaluateCondition(stack.m_41613_(), noSpace.substring(5));
        }
        if (lowNoSpace.startsWith("cantidad")) {
            return evaluateCondition(stack.m_41613_(), noSpace.substring(8));
        }

        // Numeric conditions: damage / dano
        if (lowNoSpace.startsWith("damage")) {
            return evaluateCondition(stack.m_41773_(), noSpace.substring(6));
        }
        if (lowNoSpace.startsWith("dano")) {
            return evaluateCondition(stack.m_41773_(), noSpace.substring(4));
        }

        // Numeric condition: durabilidad_pct / durability_pct (0 to 100)
        if (lowNoSpace.startsWith("durabilidad_pct") || lowNoSpace.startsWith("durability_pct")) {
            int prefixLen = lowNoSpace.startsWith("durabilidad_pct") ? 15 : 14;
            int maxDmg = stack.m_41776_();
            int pct = maxDmg > 0 ? Math.max(0, Math.round(((maxDmg - stack.m_41773_()) * 100.0f) / maxDmg)) : 100;
            return evaluateCondition(pct, noSpace.substring(prefixLen));
        }

        // LIKE conditions: name / nombre
        if (lowNoSpace.startsWith("namelike") || lowNoSpace.startsWith("nombrelike")) {
            String pattern = extractLikePattern(cleanCond);
            if ((pattern.contains("armor") || pattern.contains("armadura") || pattern.contains("casco") || pattern.contains("pechera") || pattern.contains("greba") || pattern.contains("bota") || pattern.contains("escudo")) && isArmor(stack)) return true;
            if ((pattern.contains("weapon") || (pattern.contains("arma") && !pattern.contains("armadura")) || pattern.contains("espada") || pattern.contains("sword") || pattern.contains("arco") || pattern.contains("ballesta")) && isWeapon(stack)) return true;
            if ((pattern.contains("tool") || pattern.contains("herramienta") || pattern.contains("pico") || pattern.contains("hacha") || pattern.contains("pala") || pattern.contains("azada")) && isTool(stack)) return true;
            if ((pattern.contains("book") || pattern.contains("libro")) && isBook(stack)) return true;
            if ((pattern.contains("food") || pattern.contains("comida")) && stack.m_41720_().m_41472_()) return true;
            if ((pattern.contains("potion") || pattern.contains("pocion")) && isPotion(stack)) return true;
            if ((pattern.contains("mineral") || pattern.contains("ingot") || pattern.contains("gem") || pattern.contains("dust") || pattern.contains("raw")) && isMineral(stack)) return true;
            if ((pattern.contains("block") || pattern.contains("bloque")) && isBlock(stack)) return true;

            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
            String regPath = rl != null ? rl.m_135815_().toLowerCase(Locale.ROOT) : "";
            String disp = stack.m_41786_().getString().toLowerCase(Locale.ROOT);
            return matchesPattern(regPath, pattern) || matchesPattern(disp, pattern);
        }

        // LIKE conditions: mod
        if (lowNoSpace.startsWith("modlike")) {
            String pattern = extractLikePattern(cleanCond);
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
            return rl != null && matchesPattern(rl.m_135827_().toLowerCase(Locale.ROOT), pattern);
        }

        // LIKE conditions: tag / etiqueta / mineral
        if (lowNoSpace.startsWith("taglike") || lowNoSpace.startsWith("etiquetalike") || lowNoSpace.startsWith("minerallike") || lowNoSpace.startsWith("mineral")) {
            String pattern = extractLikePattern(cleanCond);
            if ((pattern.contains("armor") || pattern.contains("armadura")) && isArmor(stack)) return true;
            if ((pattern.contains("weapon") || (pattern.contains("arma") && !pattern.contains("armadura"))) && isWeapon(stack)) return true;
            if ((pattern.contains("tool") || pattern.contains("herramienta")) && isTool(stack)) return true;
            if ((pattern.contains("book") || pattern.contains("libro")) && isBook(stack)) return true;
            if ((pattern.contains("potion") || pattern.contains("pocion")) && isPotion(stack)) return true;
            if ((pattern.contains("mineral") || pattern.contains("ingot") || pattern.contains("gem") || pattern.contains("dust") || pattern.contains("raw")) && isMineral(stack)) return true;
            if ((pattern.contains("block") || pattern.contains("bloque")) && isBlock(stack)) return true;

            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
            if (rl != null && matchesPattern(rl.m_135815_().toLowerCase(Locale.ROOT), pattern)) return true;

            return stack.m_204131_().anyMatch(t ->
                    matchesPattern(t.f_203868_().m_135815_().toLowerCase(Locale.ROOT), pattern)
                            || matchesPattern(t.f_203868_().toString().toLowerCase(Locale.ROOT), pattern));
        }

        // Equality conditions: name / nombre
        if (lowNoSpace.startsWith("name=") || lowNoSpace.startsWith("nombre=") || lowNoSpace.startsWith("item_id=")) {
            String val = extractEqualsValue(noSpace);
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
            String disp = stack.m_41786_().getString().toLowerCase(Locale.ROOT);
            return (rl != null && (rl.m_135815_().toLowerCase(Locale.ROOT).contains(val) || rl.toString().toLowerCase(Locale.ROOT).contains(val)))
                    || disp.contains(val);
        }

        // Equality conditions: mod
        if (lowNoSpace.startsWith("mod=")) {
            String val = extractEqualsValue(noSpace);
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
            return rl != null && rl.m_135827_().toLowerCase(Locale.ROOT).contains(val);
        }

        // Equality conditions: tag / etiqueta
        if (lowNoSpace.startsWith("tag=") || lowNoSpace.startsWith("etiqueta=")) {
            String val = extractEqualsValue(noSpace);
            return stack.m_204131_().anyMatch(t ->
                    t.f_203868_().m_135815_().toLowerCase(Locale.ROOT).contains(val)
                            || t.f_203868_().toString().toLowerCase(Locale.ROOT).contains(val));
        }

        // Equality condition: rareza / rarity
        if (lowNoSpace.startsWith("rareza=") || lowNoSpace.startsWith("rarity=")) {
            String val = extractEqualsValue(noSpace);
            return stack.m_41791_().name().equalsIgnoreCase(val);
        }

        // Boolean condition: encantado / enchanted
        if (lowNoSpace.contains("encantado") || lowNoSpace.contains("enchanted")) {
            boolean expected = !lowNoSpace.contains("false") && !lowNoSpace.contains("0") && !lowNoSpace.contains("no");
            boolean isEnch = stack.m_41793_() || (stack.m_41783_() != null && stack.m_41783_().m_128441_("StoredEnchantments"));
            return isEnch == expected;
        }

        // Boolean condition: apilable / stackable (tolerates "apilable = false", "apilable false =", "stackable = false")
        if (lowNoSpace.contains("apilable") || lowNoSpace.contains("stackable")) {
            boolean expected = !lowNoSpace.contains("false") && !lowNoSpace.contains("0") && !lowNoSpace.contains("no");
            return stack.m_41753_() == expected;
        }

        // Boolean condition: is_food / es_comida
        if (lowNoSpace.contains("is_food") || lowNoSpace.contains("es_comida") || lowNoSpace.contains("comida=")) {
            boolean expected = !lowNoSpace.contains("false") && !lowNoSpace.contains("0") && !lowNoSpace.contains("no");
            return stack.m_41720_().m_41472_() == expected;
        }

        // Boolean condition: es_armadura / is_armor (MUST be evaluated before es_arma because "es_armadura" contains "es_arma"!)
        if (lowNoSpace.contains("es_armadura") || lowNoSpace.contains("es_armaduras") || lowNoSpace.contains("is_armor") || lowNoSpace.contains("is_armour")) {
            boolean expected = !lowNoSpace.contains("false") && !lowNoSpace.contains("0") && !lowNoSpace.contains("no");
            return isArmor(stack) == expected;
        }

        // Boolean condition: es_arma / is_weapon
        if (lowNoSpace.contains("es_arma") || lowNoSpace.contains("es_armas") || lowNoSpace.contains("is_weapon") || lowNoSpace.contains("is_weapons")) {
            boolean expected = !lowNoSpace.contains("false") && !lowNoSpace.contains("0") && !lowNoSpace.contains("no");
            return isWeapon(stack) == expected;
        }

        // Boolean condition: es_herramienta / is_tool
        if (lowNoSpace.contains("es_herramienta") || lowNoSpace.contains("es_herramientas") || lowNoSpace.contains("is_tool") || lowNoSpace.contains("is_tools")) {
            boolean expected = !lowNoSpace.contains("false") && !lowNoSpace.contains("0") && !lowNoSpace.contains("no");
            return isTool(stack) == expected;
        }

        // Boolean condition: es_libro / is_book
        if (lowNoSpace.contains("es_libro") || lowNoSpace.contains("es_libros") || lowNoSpace.contains("is_book") || lowNoSpace.contains("is_books")) {
            boolean expected = !lowNoSpace.contains("false") && !lowNoSpace.contains("0") && !lowNoSpace.contains("no");
            return isBook(stack) == expected;
        }

        // Boolean condition: es_mineral / is_mineral
        if (lowNoSpace.contains("es_mineral") || lowNoSpace.contains("es_minerales") || lowNoSpace.contains("is_mineral") || lowNoSpace.contains("is_minerals")) {
            boolean expected = !lowNoSpace.contains("false") && !lowNoSpace.contains("0") && !lowNoSpace.contains("no");
            return isMineral(stack) == expected;
        }

        // Boolean condition: es_pocion / is_potion
        if (lowNoSpace.contains("es_pocion") || lowNoSpace.contains("es_pociones") || lowNoSpace.contains("is_potion") || lowNoSpace.contains("is_potions")) {
            boolean expected = !lowNoSpace.contains("false") && !lowNoSpace.contains("0") && !lowNoSpace.contains("no");
            return isPotion(stack) == expected;
        }

        // Boolean condition: es_netherite / is_netherite
        if (lowNoSpace.contains("es_netherite") || lowNoSpace.contains("is_netherite") || lowNoSpace.contains("netherite=")) {
            boolean expected = !lowNoSpace.contains("false") && !lowNoSpace.contains("0") && !lowNoSpace.contains("no");
            return isNetherite(stack) == expected;
        }

        // Boolean condition: es_bloque / is_block
        if (lowNoSpace.contains("es_bloque") || lowNoSpace.contains("is_block")) {
            boolean expected = !lowNoSpace.contains("false") && !lowNoSpace.contains("0") && !lowNoSpace.contains("no");
            return isBlock(stack) == expected;
        }

        // Multiplo / lote / batch / step condition: e.g. "lote = 9", "multiplo_de = 9", "batch = 9"
        if (lowNoSpace.contains("lote=") || lowNoSpace.contains("batch=") || lowNoSpace.contains("multiplo") || lowNoSpace.contains("step=")) {
            int eqIdx = noSpace.indexOf('=');
            if (eqIdx != -1) {
                try {
                    int b = Integer.parseInt(noSpace.substring(eqIdx + 1).trim());
                    return stack.m_41613_() >= b;
                } catch (Exception ignored) {}
            }
            return true;
        }

        return true;
    }

    public static boolean isWeapon(ItemStack stack) {
        if (stack == null || stack.m_41619_()) return false;
        if (stack.m_41720_() instanceof net.minecraft.world.item.SwordItem) return true;
        if (stack.m_41720_() instanceof net.minecraft.world.item.BowItem) return true;
        if (stack.m_41720_() instanceof net.minecraft.world.item.CrossbowItem) return true;
        if (stack.m_41720_() instanceof net.minecraft.world.item.TridentItem) return true;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        String id = rl != null ? rl.m_135815_().toLowerCase(Locale.ROOT) : "";
        if (id.endsWith("_sword") || id.equals("sword") ||
            id.endsWith("_bow") || id.equals("bow") ||
            id.endsWith("_crossbow") || id.equals("crossbow") ||
            id.contains("trident") || id.contains("mace") ||
            id.contains("dagger") || id.contains("spear") ||
            id.contains("katana") || id.contains("rapier") ||
            id.contains("weapon")) {
            return true;
        }
        return stack.m_204131_().anyMatch(t -> {
            String path = t.f_203868_().m_135815_().toLowerCase(Locale.ROOT);
            return path.contains("sword") || path.contains("weapon") ||
                   path.contains("bow") || path.contains("crossbow") ||
                   path.contains("trident");
        });
    }

    public static boolean isArmor(ItemStack stack) {
        if (stack == null || stack.m_41619_()) return false;
        if (stack.m_41720_() instanceof net.minecraft.world.item.ArmorItem) return true;
        if (stack.m_41720_() instanceof net.minecraft.world.item.ShieldItem) return true;
        if (stack.m_41720_() instanceof net.minecraft.world.item.ElytraItem) return true;
        if (stack.m_41720_() instanceof net.minecraft.world.item.Equipable && !(stack.m_41720_() instanceof net.minecraft.world.item.BlockItem)) return true;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        String id = rl != null ? rl.m_135815_().toLowerCase(Locale.ROOT) : "";
        if (id.endsWith("_helmet") || id.endsWith("_chestplate") ||
            id.endsWith("_leggings") || id.endsWith("_boots") ||
            id.contains("shield") || id.contains("elytra") ||
            id.contains("armor") || id.contains("horse_armor") ||
            id.contains("casco") || id.contains("pechera") ||
            id.contains("greba") || id.contains("bota") || id.contains("escudo")) {
            return true;
        }
        return stack.m_204131_().anyMatch(t -> {
            String path = t.f_203868_().m_135815_().toLowerCase(Locale.ROOT);
            return path.contains("armor") || path.contains("helmet") ||
                   path.contains("chestplate") || path.contains("legging") ||
                   path.contains("boot") || path.contains("shield");
        });
    }

    public static boolean isTool(ItemStack stack) {
        if (stack == null || stack.m_41619_()) return false;
        if (stack.m_41720_() instanceof net.minecraft.world.item.DiggerItem) return true;
        if (stack.m_41720_() instanceof net.minecraft.world.item.ShearsItem) return true;
        if (stack.m_41720_() instanceof net.minecraft.world.item.FishingRodItem) return true;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        String id = rl != null ? rl.m_135815_().toLowerCase(Locale.ROOT) : "";
        if (id.endsWith("_pickaxe") || id.endsWith("_axe") ||
            id.endsWith("_shovel") || id.endsWith("_hoe") ||
            id.contains("shears") || id.contains("fishing_rod") ||
            id.contains("flint_and_steel") || id.contains("wrench") ||
            id.contains("drill") || id.contains("tool")) {
            return true;
        }
        return stack.m_204131_().anyMatch(t -> {
            String path = t.f_203868_().m_135815_().toLowerCase(Locale.ROOT);
            return path.contains("tool") || path.contains("pickaxe") ||
                   path.contains("axe") || path.contains("shovel") ||
                   path.contains("hoe") || path.contains("shears");
        });
    }

    public static boolean isBook(ItemStack stack) {
        if (stack == null || stack.m_41619_()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        String id = rl != null ? rl.m_135815_().toLowerCase(Locale.ROOT) : "";
        if (id.contains("book") || id.contains("tome") || id.contains("guide")) {
            return true;
        }
        String disp = stack.m_41786_().getString().toLowerCase(Locale.ROOT);
        if (disp.contains("book") || disp.contains("libro")) return true;
        return stack.m_204131_().anyMatch(t -> {
            String path = t.f_203868_().m_135815_().toLowerCase(Locale.ROOT);
            return path.contains("book");
        });
    }

    public static boolean isPotion(ItemStack stack) {
        if (stack == null || stack.m_41619_()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        String id = rl != null ? rl.m_135815_().toLowerCase(Locale.ROOT) : "";
        return id.contains("potion") || id.contains("bottle_o_enchanting");
    }

    public static boolean isMineral(ItemStack stack) {
        if (stack == null || stack.m_41619_()) return false;
        if (isArmor(stack) || isWeapon(stack) || isTool(stack)) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        String id = rl != null ? rl.m_135815_().toLowerCase(Locale.ROOT) : "";
        if (id.contains("ingot") || id.contains("raw_") || id.contains("ore") ||
            id.contains("gem") || id.contains("dust") || id.contains("nugget") ||
            id.contains("diamond") || id.contains("emerald") || id.contains("lapis") ||
            id.contains("redstone") || id.contains("quartz") || id.contains("amethyst")) {
            return true;
        }
        return stack.m_204131_().anyMatch(t -> {
            String path = t.f_203868_().m_135815_().toLowerCase(Locale.ROOT);
            return path.contains("ingot") || path.contains("raw_materials") ||
                   path.contains("gems") || path.contains("dusts") ||
                   path.contains("ores") || path.contains("nuggets");
        });
    }

    public static boolean isNetherite(ItemStack stack) {
        if (stack == null || stack.m_41619_()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        String id = rl != null ? rl.m_135815_().toLowerCase(Locale.ROOT) : "";
        if (id.contains("netherite") || id.contains("inframundita")) return true;
        String disp = stack.m_41786_().getString().toLowerCase(Locale.ROOT);
        if (disp.contains("netherite") || disp.contains("inframundita")) return true;
        return stack.m_204131_().anyMatch(t -> {
            String s = t.f_203868_().toString().toLowerCase(Locale.ROOT);
            return s.contains("netherite") || s.contains("inframundita");
        });
    }

    public static boolean isBlock(ItemStack stack) {
        if (stack == null || stack.m_41619_()) return false;
        return stack.m_41720_() instanceof net.minecraft.world.item.BlockItem;
    }

    private static int extractBatchSize(String whereClause, String cleanQuery) {
        int batch = 0;
        if (whereClause != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)\\b(?:multiplo_de|multiplo|lote|batch|step)\\s*=\\s*(\\d+)").matcher(whereClause);
            if (m.find()) {
                try {
                    batch = Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (batch <= 0 && cleanQuery != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)\\b(?:LOTE|BATCH|STEP|MULTIPLO)\\s+(\\d+)\\b").matcher(cleanQuery);
            if (m.find()) {
                try {
                    batch = Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) {}
            }
        }
        return batch;
    }

    private static String extractLikePattern(String cond) {
        int likeIdx = cond.toUpperCase(Locale.ROOT).indexOf("LIKE");
        String raw = cond.substring(likeIdx + 4).trim();
        return raw.replace("'", "").replace("\"", "").toLowerCase(Locale.ROOT);
    }

    private static String extractEqualsValue(String noSpace) {
        int eqIdx = noSpace.indexOf('=');
        return noSpace.substring(eqIdx + 1).replace("'", "").replace("\"", "").toLowerCase(Locale.ROOT);
    }

    private static boolean parseSqlBoolean(String val) {
        return val.equalsIgnoreCase("true") || val.equals("1") || val.equalsIgnoreCase("si") || val.equalsIgnoreCase("yes");
    }

    private static boolean matchesPattern(String text, String pattern) {
        if (pattern.startsWith("%") && pattern.endsWith("%") && pattern.length() >= 2) {
            return text.contains(pattern.substring(1, pattern.length() - 1));
        }
        if (pattern.startsWith("%")) {
            return text.endsWith(pattern.substring(1));
        }
        if (pattern.endsWith("%")) {
            return text.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return text.equals(pattern);
    }

    private static boolean evaluateCondition(int value, String expr) {
        try {
            if (expr.startsWith(">=")) return value >= Integer.parseInt(expr.substring(2));
            if (expr.startsWith("<=")) return value <= Integer.parseInt(expr.substring(2));
            if (expr.startsWith("!=")) return value != Integer.parseInt(expr.substring(2));
            if (expr.startsWith(">")) return value > Integer.parseInt(expr.substring(1));
            if (expr.startsWith("<")) return value < Integer.parseInt(expr.substring(1));
            if (expr.startsWith("=")) return value == Integer.parseInt(expr.substring(1));
        } catch (Exception ignored) {
        }
        return false;
    }

    private static void applyOrder(List<RowMatch> matches, String orderClause) {
        String[] parts = orderClause.split("\\s+");
        String col = parts[0].toLowerCase(Locale.ROOT);
        boolean desc = parts.length > 1 && parts[1].equalsIgnoreCase("DESC");

        matches.sort((a, b) -> {
            int cmp = switch (col) {
                case "count", "cantidad" -> Integer.compare(a.stack.m_41613_(), b.stack.m_41613_());
                case "damage", "dano" -> Integer.compare(a.stack.m_41773_(), b.stack.m_41773_());
                case "rareza", "rarity" -> Integer.compare(a.stack.m_41791_().ordinal(), b.stack.m_41791_().ordinal());
                case "mod" -> {
                    ResourceLocation rla = ForgeRegistries.ITEMS.getKey(a.stack.m_41720_());
                    ResourceLocation rlb = ForgeRegistries.ITEMS.getKey(b.stack.m_41720_());
                    String ma = rla != null ? rla.m_135827_() : "";
                    String mb = rlb != null ? rlb.m_135827_() : "";
                    yield ma.compareTo(mb);
                }
                default -> {
                    ResourceLocation rla = ForgeRegistries.ITEMS.getKey(a.stack.m_41720_());
                    ResourceLocation rlb = ForgeRegistries.ITEMS.getKey(b.stack.m_41720_());
                    yield String.valueOf(rla).compareTo(String.valueOf(rlb));
                }
            };
            return desc ? -cmp : cmp;
        });
    }

    public static void returnItems(Level level, BlockPos outputBoxPos, ServerPlayer player) {
        BlockEntity be = level.m_7702_(outputBoxPos);
        if (!(be instanceof SqlOutputBoxBlockEntity outBox)) return;

        ItemStackHandler outHandler = outBox.getItemHandler();
        int returnedStacks = 0;
        Set<BlockPos> returnOrigins = new HashSet<>();

        for (int i = 0; i < outHandler.getSlots(); i++) {
            ItemStack stack = outHandler.getStackInSlot(i);
            if (!stack.m_41619_() && stack.m_41782_()) {
                CompoundTag tag = stack.m_41783_();
                if (tag != null && tag.m_128441_("SqlOriginX") && tag.m_128441_("SqlOriginY") && tag.m_128441_("SqlOriginZ")) {
                    BlockPos originPos = new BlockPos(tag.m_128451_("SqlOriginX"), tag.m_128451_("SqlOriginY"), tag.m_128451_("SqlOriginZ"));
                    BlockEntity originBe = level.m_7702_(originPos);
                    if (originBe != null) {
                        LazyOptional<IItemHandler> cap = InventoryHelper.getInventory(level, originPos, originBe, player);
                        if (cap.isPresent()) {
                            IItemHandler originHandler = cap.orElseThrow(IllegalStateException::new);
                            ItemStack extracted = outHandler.extractItem(i, stack.m_41613_(), false);
                            extracted.m_41749_("SqlOriginX");
                            extracted.m_41749_("SqlOriginY");
                            extracted.m_41749_("SqlOriginZ");
                            extracted.m_41749_("SqlOriginSlot");
                            if (extracted.m_41783_() != null && extracted.m_41783_().m_128456_()) {
                                extracted.m_41751_(null);
                            }
                            ItemStack remainder = ItemHandlerHelper.insertItem(originHandler, extracted, false);
                            if (!remainder.m_41619_()) {
                                CompoundTag remTag = remainder.m_41784_();
                                remTag.m_128405_("SqlOriginX", originPos.m_123341_());
                                remTag.m_128405_("SqlOriginY", originPos.m_123342_());
                                remTag.m_128405_("SqlOriginZ", originPos.m_123343_());
                                outHandler.insertItem(i, remainder, false);
                            } else {
                                returnedStacks++;
                                returnOrigins.add(originPos);
                            }
                        }
                    }
                }
            }
        }

        if (returnedStacks > 0) {
            if (level instanceof ServerLevel sLevel) {
                for (BlockPos origPos : returnOrigins) {
                    com.rojoh.sqlmod.util.VfxHelper.spawnTransferBeam(sLevel, outputBoxPos, origPos);
                }
                com.rojoh.sqlmod.util.VfxHelper.playTerminalSuccess(sLevel, outputBoxPos);
            }
            if (player != null) {
                player.m_5661_(Component.m_237113_("§aReturned items to their origin chests."), true);
            }
        } else if (player != null) {
            player.m_5661_(Component.m_237113_("§eNo returnable items found in Output Box."), true);
        }
    }

    private static void executeTriggerCreate(SqlTerminalBlockEntity terminal, String cleanQuery, ServerPlayer player) {
        SqlTrigger trigger = SqlTrigger.parse(cleanQuery);
        if (trigger == null) {
            sendResponse(player, "Syntax Error. Use: CREATE TRIGGER <name> ON <source> [WHERE <cond>] DO <action>");
            return;
        }
        boolean updated = terminal.getTriggerByName(trigger.name) != null;
        if (updated) {
            terminal.removeTrigger(trigger.name);
        }
        terminal.addTrigger(trigger);
        syncTriggersToClient(terminal, player);
        sendResponse(player, "Trigger '" + trigger.name + (updated ? "' actualizado con éxito." : "' creado con éxito."));
    }

    private static void executeTriggerShow(SqlTerminalBlockEntity terminal, ServerPlayer player) {
        List<SqlTrigger> triggers = terminal.getTriggers();
        if (triggers.isEmpty()) {
            sendResponse(player, "No triggers registered on this terminal.");
            return;
        }
        StringBuilder sb = new StringBuilder("Active Triggers (" + triggers.size() + "):");
        for (int i = 0; i < triggers.size(); i++) {
            sb.append("\n").append(i + 1).append(". ").append(triggers.get(i).toString());
        }
        sendResponse(player, sb.toString());
    }

    private static void executeTriggerDrop(SqlTerminalBlockEntity terminal, String cleanQuery, ServerPlayer player) {
        String name = cleanQuery.substring("DROP TRIGGER".length()).trim();
        if (terminal.removeTrigger(name)) {
            syncTriggersToClient(terminal, player);
            sendResponse(player, "Trigger '" + name + "' dropped.");
        } else {
            sendResponse(player, "Error: Trigger '" + name + "' not found.");
        }
    }

    private static void executeTriggerSetEnabled(SqlTerminalBlockEntity terminal, String cleanQuery, ServerPlayer player, boolean enabled) {
        String prefix = enabled ? "ENABLE TRIGGER" : "DISABLE TRIGGER";
        String name = cleanQuery.substring(prefix.length()).trim();
        SqlTrigger trigger = terminal.getTriggerByName(name);
        if (trigger == null) {
            sendResponse(player, "Error: Trigger '" + name + "' not found.");
            return;
        }
        trigger.enabled = enabled;
        terminal.m_6596_();
        syncTriggersToClient(terminal, player);
        sendResponse(player, "Trigger '" + name + "' " + (enabled ? "enabled" : "disabled") + ".");
    }

    private static void sendResponse(ServerPlayer player, String msg) {
        if (player == null) return;
        SqlModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new QueryResponseS2CPacket(msg));
    }

    private static void syncTriggersToClient(SqlTerminalBlockEntity terminal, ServerPlayer player) {
        if (player == null) return;
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (SqlTrigger t : terminal.getTriggers()) {
            map.put(t.name, t.enabled);
        }
        SqlModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncTriggersS2CPacket(map));
    }
}
