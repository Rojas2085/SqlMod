package com.rojoh.sqlmod.network.packet;

import com.rojoh.sqlmod.block.entity.PortableTerminalBlockEntity;
import com.rojoh.sqlmod.block.entity.SqlTerminalBlockEntity;
import com.rojoh.sqlmod.network.SqlModPacketHandler;
import com.rojoh.sqlmod.sql.SqlEngine;
import com.rojoh.sqlmod.util.InventoryHelper;
import com.rojoh.sqlmod.world.data.InfectionNetworkManager;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

public class ExecuteQueryC2SPacket {
    private final BlockPos terminalPos;
    private final String query;

    public ExecuteQueryC2SPacket(BlockPos terminalPos, String query) {
        this.terminalPos = terminalPos;
        this.query = query;
    }

    public ExecuteQueryC2SPacket(FriendlyByteBuf buf) {
        this.terminalPos = buf.m_130135_();
        this.query = buf.m_130277_();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.m_130064_(this.terminalPos);
        buf.m_130070_(this.query);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.m_284548_();
            BlockEntity be = level.m_7702_(this.terminalPos);

            String clean = this.query.trim();
            if (clean.endsWith(";")) {
                clean = clean.substring(0, clean.length() - 1).trim();
            }
            String upper = clean.toUpperCase(Locale.ROOT);

            if (be instanceof SqlTerminalBlockEntity sqlTerm) {
                if (!sqlTerm.isOwner(player.m_20148_())) {
                    player.m_5661_(Component.m_237113_("§cAcceso denegado. Propietario: " + sqlTerm.getOwnerName()), true);
                    return;
                }
                sqlTerm.setLastQuery(this.query);
                if (upper.equals("SHOW TABLES") || upper.equals("SHOW CHESTS")) {
                    Map<String, BlockPos> chests = sqlTerm.getLinkedChests();
                    if (chests.isEmpty()) {
                        sendLine(player, "§eNo hay cofres vinculados a esta terminal.");
                    } else {
                        sendLine(player, "§a=== Cofres Vinculados (" + chests.size() + ") ===");
                        for (Map.Entry<String, BlockPos> entry : chests.entrySet()) {
                            BlockPos cp = entry.getValue();
                            sendLine(player, "§b• " + entry.getKey() + " §7[" + cp.m_123341_() + ", " + cp.m_123342_() + ", " + cp.m_123343_() + "]");
                        }
                    }
                    return;
                }
                if (upper.equals("HELP")) {
                    sendLine(player, "§a=== Comandos SQL Disponibles ===");
                    sendLine(player, "§7• SELECT * FROM <cofre|*> [WHERE ...] [ORDER BY ...] [LIMIT n]");
                    sendLine(player, "§7• SELECT mod, SUM(cantidad) FROM * GROUP BY mod");
                    sendLine(player, "§7• INSERT INTO <cofre> [SELECT * FROM ...]");
                    sendLine(player, "§7• DELETE FROM <cofre> [WHERE ...]");
                    sendLine(player, "§7• OPTIMIZE TABLE <cofre|*> (compacta stacks)");
                    sendLine(player, "§7• ALTER TABLE <cofre|*> ORDER BY <nombre|cantidad|rareza> [ASC|DESC]");
                    sendLine(player, "§7• DISTRIBUTE FROM <origen> INTO (c1, c2) [WHERE ...]");
                    sendLine(player, "§7• ROLLBACK | COMMIT TO <cofre>");
                    sendLine(player, "§7• CREATE VIEW <v> AS SELECT ... | CREATE MACRO <m> AS ... | EXEC <m>");
                    sendLine(player, "§7• Columnas WHERE: name, mod, tag, count, damage, durabilidad_pct, rareza, encantado, apilable");
                    return;
                }
            } else if (be instanceof PortableTerminalBlockEntity portable) {
                portable.setLastQuery(this.query);
                if (upper.equals("HELP")) {
                    sendLine(player, "§c=== Comandos Terminal Portátil ===");
                    sendLine(player, "§7• SHOW ITEMS / SCAN (ver ítems en la red)");
                    sendLine(player, "§7• SELECT COUNT(*) FROM infectados [WHERE ...]");
                    sendLine(player, "§7• SELECT * FROM infectados [WHERE ...] [ORDER BY ...] [LIMIT n]");
                    sendLine(player, "§7• Columnas WHERE: name, mod, tag, count, damage, durabilidad_pct, rareza, encantado");
                    return;
                }
                boolean isScanCmd = upper.equals("SHOW ITEMS") || upper.equals("SCAN") || upper.equals("SHOW TABLES")
                        || upper.startsWith("SELECT COUNT") || upper.contains(" GROUP BY ");
                boolean missingOutputBox = portable.getOutputBoxPos() == null || portable.getOutputBoxDimension() == null || portable.getOutputBoxDimension().isEmpty();
                if (isScanCmd || (upper.startsWith("SELECT") && missingOutputBox)) {
                    handlePortableScan(level, portable, clean, upper, player, missingOutputBox && !isScanCmd);
                    return;
                }
            }

            SqlEngine.executeQuery(level, this.terminalPos, this.query, player);
        });
        ctx.setPacketHandled(true);
        return true;
    }

    private static void handlePortableScan(ServerLevel level, PortableTerminalBlockEntity portable, String clean, String upper, ServerPlayer player, boolean warnNoOutput) {
        UUID netId = portable.getLinkedNetworkId();
        if (netId == null) {
            sendLine(player, "§cError: Sin Llave de Red vinculada.");
            return;
        }
        InfectionNetworkManager.InfectionNetwork net = InfectionNetworkManager.get(level).getNetwork(netId);
        if (net == null || net.expiresAt <= System.currentTimeMillis()) {
            sendLine(player, "§cError: La Red de Infección ha expirado o no es válida.");
            return;
        }

        String whereClause = null;
        int whereIdx = upper.indexOf(" WHERE ");
        int groupIdx = upper.indexOf(" GROUP BY ");
        int orderIdx = upper.indexOf(" ORDER BY ");
        int limitIdx = upper.indexOf(" LIMIT ");
        if (whereIdx != -1) {
            int end = clean.length();
            if (groupIdx != -1 && groupIdx < end) end = groupIdx;
            if (orderIdx != -1 && orderIdx < end) end = orderIdx;
            if (limitIdx != -1 && limitIdx < end) end = limitIdx;
            whereClause = clean.substring(whereIdx + 7, end).trim();
        }
        String groupCol = null;
        if (groupIdx != -1) {
            int end = clean.length();
            if (orderIdx != -1 && orderIdx < end) end = orderIdx;
            if (limitIdx != -1 && limitIdx < end) end = limitIdx;
            groupCol = clean.substring(groupIdx + 10, end).trim().toLowerCase(Locale.ROOT);
        }

        final String finalWhere = whereClause;
        final String finalGroup = groupCol;
        Map<String, Integer> counts = new LinkedHashMap<>();
        int[] totalItems = new int[]{0};

        for (BlockPos chestPos : net.chestPositions) {
            BlockEntity cbe = level.m_7702_(chestPos);
            if (cbe != null) {
                InventoryHelper.getInventory(level, chestPos, cbe, player).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.m_41619_() && SqlEngine.matchesWherePublic(stack, finalWhere)) {
                            String key;
                            if ("mod".equals(finalGroup)) {
                                key = ForgeRegistries.ITEMS.getKey(stack.m_41720_()) != null
                                        ? ForgeRegistries.ITEMS.getKey(stack.m_41720_()).m_135827_()
                                        : "unknown";
                            } else if ("rareza".equals(finalGroup) || "rarity".equals(finalGroup)) {
                                key = stack.m_41791_().name().toLowerCase(Locale.ROOT);
                            } else {
                                key = ForgeRegistries.ITEMS.getKey(stack.m_41720_()) != null
                                        ? ForgeRegistries.ITEMS.getKey(stack.m_41720_()).m_135815_()
                                        : stack.m_41786_().getString();
                            }
                            counts.put(key, counts.getOrDefault(key, 0) + stack.m_41613_());
                            totalItems[0] += stack.m_41613_();
                        }
                    }
                });
            }
        }

        if (upper.startsWith("SELECT COUNT")) {
            sendLine(player, "§aCOUNT(*): §e" + totalItems[0] + " ítems §7(" + counts.size() + " tipos en " + net.chestPositions.size() + " cofres)");
            return;
        }

        if (warnNoOutput) {
            sendLine(player, "§6[Modo Vista Previa - Sin Output Box vinculada]");
        }
        if (counts.isEmpty()) {
            sendLine(player, "§7Red escaneada: 0 ítems coinciden.");
            return;
        }
        sendLine(player, "§a=== Red: " + totalItems[0] + " ítems (" + counts.size() + " grupos/tipos) ===");
        int shown = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            sendLine(player, "§b• " + e.getKey() + ": §e" + e.getValue());
            shown++;
            if (shown >= 12 && counts.size() > 12) {
                sendLine(player, "§7... y " + (counts.size() - 12) + " más.");
                break;
            }
        }
    }

    private static void sendLine(ServerPlayer player, String msg) {
        SqlModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new QueryResponseS2CPacket(msg));
    }
}
