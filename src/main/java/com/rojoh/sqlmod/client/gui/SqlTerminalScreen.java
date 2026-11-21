package com.rojoh.sqlmod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.rojoh.sqlmod.network.SqlModPacketHandler;
import com.rojoh.sqlmod.network.packet.ExecuteQueryC2SPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SqlTerminalScreen extends AbstractContainerScreen<SqlTerminalMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("sqlmod", "textures/gui/sql_terminal.png");
    private static final int CONSOLE_W = 330;
    private static final int PANEL_W = 130;
    private static final int TRIG_PANEL_W = 130;
    private static final int GUI_H = 220;

    private EditBox queryBox;
    private static final List<String> history = new ArrayList<>();
    private static final List<String> queryHistory = new ArrayList<>();
    private static final String[] favorites = new String[]{
            "SHOW TABLES",
            "SELECT * FROM * WHERE es_mineral = true",
            "SHOW TRIGGERS"
    };
    private static SqlTerminalScreen currentScreen;
    private static int scrollOffsetConsole = 0;
    private int historyIndex = -1;
    private double scrollOffsetTables = 0.0D;
    private double scrollOffsetTriggers = 0.0D;

    public SqlTerminalScreen(SqlTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.f_97726_ = 590;
        this.f_97727_ = 220;
    }

    private String getFirstChestName() {
        Map<String, BlockPos> chests = this.f_97732_.getLinkedChests();
        return chests.isEmpty() ? "*" : chests.keySet().iterator().next();
    }

    @Override
    protected void m_7856_() {
        super.m_7856_();
        currentScreen = this;
        int left = (this.f_96543_ - this.f_97726_) / 2;
        int top = (this.f_96544_ - this.f_97727_) / 2;

        // Quick-Action Command Dashboard Toolbar (y = top + 16, height = 15)
        int bx = left + 4;
        int by = top + 16;

        this.m_142416_(Button.m_253074_(Component.m_237113_("§bTablas"), b -> {
            this.queryBox.m_94144_("SHOW TABLES");
            this.submitQuery();
        }).m_252987_(bx, by, 44, 15).m_253136_());
        bx += 46;

        this.m_142416_(Button.m_253074_(Component.m_237113_("§aSelect"), b -> {
            this.queryBox.m_94144_("SELECT * FROM " + getFirstChestName() + " WHERE ");
            this.queryBox.m_93692_(true);
            this.queryBox.m_94201_();
        }).m_252987_(bx, by, 42, 15).m_253136_());
        bx += 44;

        this.m_142416_(Button.m_253074_(Component.m_237113_("§eInsert"), b -> {
            String first = getFirstChestName();
            this.queryBox.m_94144_("INSERT INTO " + first + " SELECT * FROM ");
            this.queryBox.m_93692_(true);
            this.queryBox.m_94201_();
        }).m_252987_(bx, by, 42, 15).m_253136_());
        bx += 44;

        this.m_142416_(Button.m_253074_(Component.m_237113_("§6Rollback"), b -> {
            this.queryBox.m_94144_("ROLLBACK");
            this.submitQuery();
        }).m_252987_(bx, by, 52, 15).m_253136_());
        bx += 54;

        // 3 Favorite Macro Slots (Click = load/run, Shift+Click = save current query)
        for (int i = 0; i < 3; i++) {
            final int slotIdx = i;
            this.m_142416_(Button.m_253074_(Component.m_237113_("§e★" + (i + 1)), b -> {
                if (Screen.m_96638_()) {
                    String current = this.queryBox.m_94155_().trim();
                    if (!current.isEmpty()) {
                        favorites[slotIdx] = current;
                        history.add("§e[★" + (slotIdx + 1) + "] Guardado: §f" + current);
                        scrollOffsetConsole = 0;
                    }
                } else {
                    String fav = favorites[slotIdx];
                    if (fav != null && !fav.isEmpty()) {
                        this.queryBox.m_94144_(fav);
                        this.queryBox.m_93692_(true);
                        this.queryBox.m_94201_();
                    }
                }
            }).m_252987_(bx, by, 26, 15).m_253136_());
            bx += 28;
        }

        this.m_142416_(Button.m_253074_(Component.m_237113_("§7?"), b -> {
            this.queryBox.m_94144_("HELP");
            this.submitQuery();
        }).m_252987_(left + CONSOLE_W - 22, by, 18, 15).m_253136_());

        // Input Box & Run/Clear Controls
        this.queryBox = new EditBox(this.f_96547_, left + 6, top + 220 - 27, 276, 14, Component.m_237113_("Query SQL..."));
        this.queryBox.m_94199_(4096);
        this.queryBox.m_93692_(true);
        this.m_142416_(this.queryBox);

        this.m_142416_(Button.m_253074_(Component.m_237113_("§a▶"), b -> this.submitQuery())
                .m_252987_(left + 330 - 44, top + 220 - 29, 20, 18)
                .m_253136_());

        this.m_142416_(Button.m_253074_(Component.m_237113_("§c✕"), b -> {
            history.clear();
            this.historyIndex = -1;
            scrollOffsetConsole = 0;
        }).m_252987_(left + 330 - 22, top + 220 - 29, 18, 18).m_253136_());
    }

    private void submitQuery() {
        String q = this.queryBox.m_94155_().trim();
        if (!q.isEmpty()) {
            history.add("§a> §f" + q);
            scrollOffsetConsole = 0;
            if (queryHistory.isEmpty() || !queryHistory.get(0).equals(q)) {
                queryHistory.add(0, q);
                if (queryHistory.size() > 30) {
                    queryHistory.remove(queryHistory.size() - 1);
                }
            }
            this.historyIndex = -1;
            SqlModPacketHandler.INSTANCE.sendToServer(new ExecuteQueryC2SPacket(this.f_97732_.getBlockEntity().m_58899_(), q));
            this.queryBox.m_94144_("");
        }
    }

    @Override
    public boolean m_6375_(double mouseX, double mouseY, int button) {
        int left = (this.f_96543_ - this.f_97726_) / 2;
        int top = (this.f_96544_ - this.f_97727_) / 2;
        int p1 = left + CONSOLE_W;
        int p2 = p1 + PANEL_W;

        // Clickable Tables Panel
        if (button == 0 && mouseX >= p1 + 2 && mouseX <= p1 + PANEL_W - 6 && mouseY >= top + 18 && mouseY <= top + 185) {
            Map<String, BlockPos> chests = this.f_97732_.getLinkedChests();
            BlockPos outPos = this.f_97732_.getOutputBoxPos();
            int maxRows = outPos != null ? 6 : 7;
            int row = (int) ((mouseY - (top + 18)) / 24);
            if (row >= 0 && row < maxRows) {
                int targetIdx = (int) this.scrollOffsetTables + row;
                int idx = 0;
                for (String tableName : chests.keySet()) {
                    if (idx == targetIdx) {
                        String cur = this.queryBox.m_94155_();
                        if (cur.trim().isEmpty()) {
                            this.queryBox.m_94144_("SELECT * FROM " + tableName + " ");
                        } else {
                            String sep = cur.endsWith(" ") ? "" : " ";
                            this.queryBox.m_94144_(cur + sep + tableName + " ");
                        }
                        this.queryBox.m_93692_(true);
                        this.queryBox.m_94201_();
                        return true;
                    }
                    idx++;
                }
            }
        }

        // Clickable Triggers Panel
        if (button == 0 && mouseX >= p2 + 2 && mouseX <= p2 + TRIG_PANEL_W - 6 && mouseY >= top + 18 && mouseY <= top + 210) {
            Map<String, Boolean> trigs = this.f_97732_.getActiveTriggers();
            int row = (int) ((mouseY - (top + 18)) / 24);
            if (row >= 0 && row < 7) {
                int targetIdx = (int) this.scrollOffsetTriggers + row;
                int idx = 0;
                for (Map.Entry<String, Boolean> entry : trigs.entrySet()) {
                    if (idx == targetIdx) {
                        String cmd = (entry.getValue() ? "DISABLE TRIGGER " : "ENABLE TRIGGER ") + entry.getKey();
                        this.queryBox.m_94144_(cmd);
                        this.queryBox.m_93692_(true);
                        this.queryBox.m_94201_();
                        return true;
                    }
                    idx++;
                }
            }
        }

        return super.m_6375_(mouseX, mouseY, button);
    }

    @Override
    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        if (this.queryBox != null && this.queryBox.m_93696_()) {
            if (keyCode == 257 || keyCode == 335) {
                this.submitQuery();
                return true;
            }
            if (keyCode == 258) { // Tab autocomplete
                String raw = this.queryBox.m_94155_();
                String cur = raw.trim();
                Map<String, BlockPos> chests = this.f_97732_.getLinkedChests();
                String firstChest = chests.isEmpty() ? "*" : chests.keySet().iterator().next();
                if (cur.isEmpty()) {
                    this.queryBox.m_94144_("SELECT * FROM " + firstChest + " ");
                    this.queryBox.m_94201_();
                } else if (cur.toUpperCase().endsWith("FROM") || cur.toUpperCase().endsWith("TABLE") || cur.toUpperCase().endsWith("TO")) {
                    this.queryBox.m_94144_(cur + " " + firstChest + " ");
                    this.queryBox.m_94201_();
                } else {
                    int lastSpace = raw.lastIndexOf(' ');
                    String prefix = lastSpace == -1 ? "" : raw.substring(0, lastSpace + 1);
                    String lastWord = (lastSpace == -1 ? raw : raw.substring(lastSpace + 1)).toLowerCase();
                    if (!lastWord.isEmpty()) {
                        List<String> dict = new ArrayList<>(chests.keySet());
                        for (String kw : new String[]{
                                "SELECT", "INSERT INTO", "DELETE FROM", "OPTIMIZE TABLE", "ALTER TABLE",
                                "DISTRIBUTE FROM", "ROLLBACK", "COMMIT TO", "CREATE MACRO", "CREATE VIEW",
                                "CREATE TRIGGER", "ENABLE TRIGGER", "DISABLE TRIGGER", "EXEC", "SHOW TABLES",
                                "SHOW MACROS", "SHOW VIEWS", "SHOW TRIGGERS", "FROM", "WHERE", "ORDER BY",
                                "GROUP BY", "HAVING", "LIMIT", "LIKE", "SUM(cantidad)", "COUNT(*)", "nombre",
                                "cantidad", "dano", "durabilidad_pct", "rareza", "encantado", "apilable",
                                "etiqueta", "mod", "es_mineral", "es_arma", "es_armadura", "es_herramienta"
                        }) {
                            dict.add(kw);
                        }
                        for (String cand : dict) {
                            if (cand.toLowerCase().startsWith(lastWord) && !cand.equalsIgnoreCase(lastWord)) {
                                this.queryBox.m_94144_(prefix + cand + " ");
                                this.queryBox.m_94201_();
                                break;
                            }
                        }
                    }
                }
                return true;
            }
            if (keyCode == 265) { // Up arrow
                if (!queryHistory.isEmpty()) {
                    this.historyIndex = Math.min(this.historyIndex + 1, queryHistory.size() - 1);
                    this.queryBox.m_94144_(queryHistory.get(this.historyIndex));
                    this.queryBox.m_94201_();
                }
                return true;
            }
            if (keyCode == 264) { // Down arrow
                if (this.historyIndex > 0) {
                    this.historyIndex--;
                    this.queryBox.m_94144_(queryHistory.get(this.historyIndex));
                    this.queryBox.m_94201_();
                } else {
                    this.historyIndex = -1;
                    this.queryBox.m_94144_("");
                }
                return true;
            }
            if (keyCode == 256) {
                this.queryBox.m_93692_(false);
                return true;
            }
            this.queryBox.m_7933_(keyCode, scanCode, modifiers);
            return true;
        }
        if (this.f_96541_ != null && this.f_96541_.f_91066_.f_92092_.m_90832_(keyCode, scanCode)) {
            return true;
        }
        return super.m_7933_(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean m_6050_(double mouseX, double mouseY, double delta) {
        int left = (this.f_96543_ - this.f_97726_) / 2;
        int top = (this.f_96544_ - this.f_97727_) / 2;
        int p1 = left + 330;
        int p2 = p1 + 130;

        if (mouseX >= left && mouseX < p1 && mouseY >= top + 33 && mouseY <= top + 220 - 32) {
            int maxScroll = Math.max(0, history.size() - 15);
            int step = delta > 0 ? 1 : -1;
            scrollOffsetConsole = Math.max(0, Math.min(maxScroll, scrollOffsetConsole + step));
            return true;
        }

        if (mouseX >= p1 && mouseX <= p1 + 130 && mouseY >= top && mouseY <= top + 220) {
            Map<String, BlockPos> chests = this.f_97732_.getLinkedChests();
            BlockPos outPos = this.f_97732_.getOutputBoxPos();
            int maxRows = outPos != null ? 6 : 7;
            if (chests.size() > maxRows) {
                int maxOff = chests.size() - maxRows;
                this.scrollOffsetTables -= delta;
                if (this.scrollOffsetTables < 0.0D) this.scrollOffsetTables = 0.0D;
                if (this.scrollOffsetTables > maxOff) this.scrollOffsetTables = maxOff;
                return true;
            }
        }

        if (mouseX >= p2 && mouseX <= p2 + 130 && mouseY >= top && mouseY <= top + 220) {
            Map<String, Boolean> trigs = this.f_97732_.getActiveTriggers();
            int maxRows = 7;
            if (trigs.size() > maxRows) {
                int maxOff = trigs.size() - maxRows;
                this.scrollOffsetTriggers -= delta;
                if (this.scrollOffsetTriggers < 0.0D) this.scrollOffsetTriggers = 0.0D;
                if (this.scrollOffsetTriggers > maxOff) this.scrollOffsetTriggers = maxOff;
                return true;
            }
        }
        return super.m_6050_(mouseX, mouseY, delta);
    }

    @Override
    protected void m_7286_(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int left = (this.f_96543_ - this.f_97726_) / 2;
        int top = (this.f_96544_ - this.f_97727_) / 2;

        // Main CRT console panel
        g.m_280509_(left, top, left + 330, top + 220, -15987700);
        g.m_280509_(left, top, left + 330, top + 14, -15783409);
        g.m_280509_(left + 1, top + 14, left + 329, top + 33, -15591140); // Toolbar backdrop
        g.m_280509_(left, top + 220 - 33, left + 330, top + 220, -15461336);
        drawBorder(g, left, top, 330, 220, -16711800);
        g.m_280509_(left + 2, top + 220 - 34, left + 330 - 2, top + 220 - 33, -16711800);
        g.m_280509_(left + 2, top + 14, left + 330 - 2, top + 15, -16733628);
        g.m_280509_(left + 2, top + 33, left + 330 - 2, top + 34, -16748740);

        // Panel 1: Linked Tables (Chests)
        int p1 = left + 330;
        g.m_280509_(p1, top, p1 + 130, top + 220, -16249576);
        g.m_280509_(p1 + 1, top + 1, p1 + 130 - 1, top + 15, -16771021);
        g.m_280509_(p1 + 2, top + 15, p1 + 130 - 2, top + 16, -16755286);
        drawBorder(g, p1, top, 130, 220, -16733441);
        g.m_280509_(left + 330 - 1, top, left + 330, top + 220, -16711800);

        // Panel 2: Reactive Triggers
        int p2 = p1 + 130;
        g.m_280509_(p2, top, p2 + 130, top + 220, -15726568);
        g.m_280509_(p2 + 1, top + 1, p2 + 130 - 1, top + 15, -13434829);
        g.m_280509_(p2 + 2, top + 15, p2 + 130 - 2, top + 16, -5635841);
        drawBorder(g, p2, top, 130, 220, -5635841);
    }

    @Override
    public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(g);
        super.m_88315_(g, mouseX, mouseY, partialTick);
        this.m_280072_(g, mouseX, mouseY);
    }

    @Override
    protected void m_280003_(GuiGraphics g, int mouseX, int mouseY) {
        int left = (this.f_96543_ - this.f_97726_) / 2;
        int top = (this.f_96544_ - this.f_97727_) / 2;
        int relMX = mouseX - left;
        int relMY = mouseY - top;

        g.m_280056_(this.f_96547_, "§a◈ §a§lSQL §2Terminal §8v1.2", 4, 3, 65416, false);
        String statusHdr = "§a● ONLINE §8| §7" + history.size() + " logs";
        int hdrW = this.f_96547_.m_92895_(Component.m_237113_(statusHdr).getString());
        g.m_280056_(this.f_96547_, statusHdr, 326 - hdrW, 3, 65416, false);

        // Console Log (y = 36..184, 15 lines)
        int maxLines = 15;
        int total = history.size();
        int maxScroll = Math.max(0, total - maxLines);
        if (scrollOffsetConsole > maxScroll) scrollOffsetConsole = maxScroll;
        int endIdx = total - scrollOffsetConsole;
        int startIdx = Math.max(0, endIdx - maxLines);
        int y = 36;

        for (int i = startIdx; i < endIdx; i++) {
            String line = history.get(i);
            if (this.f_96547_.m_92895_(Component.m_237113_(line).getString()) > 312) {
                while (line.length() > 2 && this.f_96547_.m_92895_(Component.m_237113_(line + "...").getString()) > 312) {
                    line = line.substring(0, line.length() - 1);
                }
                line = line + "...";
            }
            g.m_280056_(this.f_96547_, line, 6, y, 16777215, false);
            y += 10;
        }

        if (total > maxLines) {
            int trackH = 148;
            int thumbH = Math.max(12, (maxLines * trackH) / total);
            int thumbY = 36 + (int) (((double) (maxScroll - scrollOffsetConsole) / maxScroll) * (trackH - thumbH));
            g.m_280509_(324, 36, 327, 36 + trackH, 0x44000000);
            g.m_280509_(324, thumbY, 327, thumbY + thumbH, -16711800);
        }

        g.m_280056_(this.f_96547_, "§7↑↓ hist · Tab · Clic tabla/trigger · Shift+★ guardar", 4, 210, 5592405, false);

        // Panel 1: Tables (Chests)
        int p1 = 330;
        Map<String, BlockPos> chests = this.f_97732_.getLinkedChests();
        g.m_280056_(this.f_96547_, "§b§l⊞ Tablas (" + chests.size() + ")", p1 + 5, 3, 5636095, false);
        BlockPos outPos = this.f_97732_.getOutputBoxPos();
        int py = 18;
        int maxRows = outPos != null ? 6 : 7;
        int totalChests = chests.size();

        if (chests.isEmpty()) {
            g.m_280056_(this.f_96547_, "§7(sin cofres)", p1 + 5, py + 4, 6710886, false);
        } else {
            if (totalChests > maxRows) {
                int maxOff = totalChests - maxRows;
                if (this.scrollOffsetTables > maxOff) this.scrollOffsetTables = maxOff;
                int trackH = outPos != null ? 156 : 192;
                int thumbH = Math.max(10, (maxRows * trackH) / totalChests);
                int thumbY = 18 + (int) ((this.scrollOffsetTables / maxOff) * (trackH - thumbH));
                g.m_280509_(p1 + 130 - 6, 18, p1 + 130 - 2, 18 + trackH, -15783409);
                g.m_280509_(p1 + 130 - 6, thumbY, p1 + 130 - 2, thumbY + thumbH, -16733441);
            }
            int skip = (int) this.scrollOffsetTables;
            int idx = 0;
            int drawn = 0;
            for (Map.Entry<String, BlockPos> entry : chests.entrySet()) {
                if (idx < skip) {
                    idx++;
                    continue;
                }
                if (drawn >= maxRows) break;
                int boxW = totalChests > maxRows ? 122 : 128;
                boolean hovered = relMX >= p1 + 2 && relMX <= p1 + boxW && relMY >= py && relMY <= py + 21;
                boolean isOverflow = entry.getKey().equalsIgnoreCase("overflow") || entry.getKey().equalsIgnoreCase("desborde");
                int bg = hovered ? 0x4400FFAA : 452984831;
                int bar = isOverflow ? 0xFFFFAA00 : -16711800;
                g.m_280509_(p1 + 2, py, p1 + boxW, py + 21, bg);
                g.m_280509_(p1 + 2, py, p1 + 4, py + 21, bar);
                String name = entry.getKey();
                int maxLen = totalChests > maxRows ? 12 : 15;
                if (name.length() > maxLen) name = name.substring(0, maxLen - 2) + "..";
                g.m_280056_(this.f_96547_, (isOverflow ? "§6★ " : "§e") + name, p1 + 7, py + 2, 16777045, false);
                BlockPos cp = entry.getValue();
                String coords = formatCoord(cp.m_123341_()) + "," + formatCoord(cp.m_123342_()) + "," + formatCoord(cp.m_123343_());
                g.m_280056_(this.f_96547_, isOverflow ? "§6[OVERFLOW]" : "§7" + coords, p1 + 7, py + 12, 7829367, false);
                py += 24;
                drawn++;
                idx++;
            }
        }

        if (outPos != null) {
            int oy = 190;
            g.m_280509_(p1 + 6, oy - 4, p1 + 130 - 6, oy - 3, -16764075);
            g.m_280509_(p1 + 2, oy, p1 + 130 - 2, oy + 21, 436242687);
            g.m_280509_(p1 + 2, oy, p1 + 4, oy + 21, -16733441);
            g.m_280056_(this.f_96547_, "§b◈ Output Box", p1 + 7, oy + 2, 5636095, false);
            String coords = formatCoord(outPos.m_123341_()) + "," + formatCoord(outPos.m_123342_()) + "," + formatCoord(outPos.m_123343_());
            g.m_280056_(this.f_96547_, "§7" + coords, p1 + 7, oy + 12, 7829367, false);
        }

        // Panel 2: Triggers
        int p2 = 460;
        Map<String, Boolean> trigs = this.f_97732_.getActiveTriggers();
        long onCount = trigs.values().stream().filter(v -> v).count();
        g.m_280056_(this.f_96547_, "§d§l⚡ Triggers (" + onCount + "/" + trigs.size() + ")", p2 + 5, 3, -5635841, false);
        int ty = 18;
        int maxTrigRows = 7;
        int totalTrigs = trigs.size();
        if (trigs.isEmpty()) {
            g.m_280056_(this.f_96547_, "§7(sin triggers)", p2 + 5, ty + 4, 6710886, false);
        } else {
            if (totalTrigs > maxTrigRows) {
                int maxOff = totalTrigs - maxTrigRows;
                if (this.scrollOffsetTriggers > maxOff) this.scrollOffsetTriggers = maxOff;
                int trackH = 192;
                int thumbH = Math.max(10, (maxTrigRows * trackH) / totalTrigs);
                int thumbY = 18 + (int) ((this.scrollOffsetTriggers / maxOff) * (trackH - thumbH));
                g.m_280509_(p2 + 130 - 6, 18, p2 + 130 - 2, 18 + trackH, -13434829);
                g.m_280509_(p2 + 130 - 6, thumbY, p2 + 130 - 2, thumbY + thumbH, -5635841);
            }
            int skip = (int) this.scrollOffsetTriggers;
            int idx = 0;
            int drawn = 0;
            for (Map.Entry<String, Boolean> entry : trigs.entrySet()) {
                if (idx < skip) {
                    idx++;
                    continue;
                }
                if (drawn >= maxTrigRows) break;
                int boxW = totalTrigs > maxTrigRows ? 122 : 128;
                boolean hovered = relMX >= p2 + 2 && relMX <= p2 + boxW && relMY >= ty && relMY <= ty + 21;
                g.m_280509_(p2 + 2, ty, p2 + boxW, ty + 21, hovered ? 0x44AA55FF : 452984831);
                boolean enabled = entry.getValue();
                int barCol = enabled ? -16711800 : -43691;
                g.m_280509_(p2 + 2, ty, p2 + 4, ty + 21, barCol);
                String tName = entry.getKey();
                int maxLen = totalTrigs > maxTrigRows ? 12 : 15;
                if (tName.length() > maxLen) tName = tName.substring(0, maxLen - 2) + "..";
                g.m_280056_(this.f_96547_, "§f" + tName, p2 + 7, ty + 2, 16777215, false);
                g.m_280056_(this.f_96547_, enabled ? "§a[ON] §8clic" : "§c[OFF] §8clic", p2 + 7, ty + 12, 7829367, false);
                ty += 24;
                drawn++;
                idx++;
            }
        }
    }

    private static String formatCoord(int c) {
        String s = String.valueOf(c);
        return s.length() > 5 ? s.substring(0, 4) + ".." : s;
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.m_280509_(x, y, x + w, y + 1, color);
        g.m_280509_(x, y + h - 1, x + w, y + h, color);
        g.m_280509_(x, y, x + 1, y + h, color);
        g.m_280509_(x + w - 1, y, x + w, y + h, color);
    }

    public static void appendResponse(String response) {
        history.add(response);
        scrollOffsetConsole = 0;
    }

    @Override
    public void m_7861_() {
        super.m_7861_();
        currentScreen = null;
    }
}
