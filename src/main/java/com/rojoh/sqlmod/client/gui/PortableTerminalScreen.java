package com.rojoh.sqlmod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.rojoh.sqlmod.network.SqlModPacketHandler;
import com.rojoh.sqlmod.network.packet.ExecuteQueryC2SPacket;
import com.rojoh.sqlmod.network.packet.RequestNetworkStatusC2SPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PortableTerminalScreen extends AbstractContainerScreen<PortableTerminalMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("sqlmod", "textures/gui/portable_terminal.png");
    private static final int CONSOLE_W = 330;
    private static final int PANEL_W = 175;
    private static final int GUI_H = 220;

    private EditBox queryBox;
    private static final List<String> history = new ArrayList<>();
    private static final List<String> queryHistory = new ArrayList<>();
    private static final String[] favorites = new String[]{
            "SHOW ITEMS",
            "SELECT * FROM infectados WHERE es_mineral = true",
            "SELECT * FROM infectados WHERE encantado = true"
    };
    private int historyIndex = -1;
    private int tickCounter = 0;
    private static int scrollOffsetConsole = 0;
    private static int networkedChests = 0;
    private static long networkTimeLeft = 0L;

    public PortableTerminalScreen(PortableTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.f_97726_ = 505;
        this.f_97727_ = 220;
    }

    public static void updateStatus(int chests, long timeLeft) {
        networkedChests = chests;
        networkTimeLeft = timeLeft;
    }

    public static void appendResponse(String response) {
        history.add(response);
        scrollOffsetConsole = 0;
    }

    @Override
    protected void m_7856_() {
        super.m_7856_();
        int left = (this.f_96543_ - this.f_97726_) / 2;
        int top = (this.f_96544_ - this.f_97727_) / 2;

        // Top Command Dashboard Toolbar (y = top + 16, height = 15)
        int bx = left + 4;
        int by = top + 16;

        this.m_142416_(Button.m_253074_(Component.m_237113_("§aItems"), b -> {
            this.queryBox.m_94144_("SHOW ITEMS");
            this.submitQuery();
        }).m_252987_(bx, by, 40, 15).m_253136_());
        bx += 42;

        this.m_142416_(Button.m_253074_(Component.m_237113_("§bScan"), b -> {
            this.queryBox.m_94144_("SCAN");
            this.submitQuery();
        }).m_252987_(bx, by, 38, 15).m_253136_());
        bx += 40;

        this.m_142416_(Button.m_253074_(Component.m_237113_("§eSelect"), b -> {
            this.queryBox.m_94144_("SELECT * FROM infectados WHERE ");
            this.queryBox.m_93692_(true);
            this.queryBox.m_94201_();
        }).m_252987_(bx, by, 44, 15).m_253136_());
        bx += 46;

        // 3 Favorite Macro Slots
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

        // Smart Filter Dashboard in Right Panel
        int panelX = left + 330;
        this.m_142416_(Button.m_253074_(Component.m_237113_("§a🔍 Escanear Red"), b -> {
            this.queryBox.m_94144_("SHOW ITEMS");
            this.submitQuery();
        }).m_252987_(panelX + 6, top + 106, 163, 15).m_253136_());

        this.m_142416_(Button.m_253074_(Component.m_237113_("§b💎 Minerales"), b -> {
            this.queryBox.m_94144_("SELECT * FROM infectados WHERE es_mineral = true");
            this.queryBox.m_93692_(true);
            this.queryBox.m_94201_();
        }).m_252987_(panelX + 6, top + 123, 80, 15).m_253136_());

        this.m_142416_(Button.m_253074_(Component.m_237113_("§d✨ Encant."), b -> {
            this.queryBox.m_94144_("SELECT * FROM infectados WHERE encantado = true");
            this.queryBox.m_93692_(true);
            this.queryBox.m_94201_();
        }).m_252987_(panelX + 89, top + 123, 80, 15).m_253136_());

        this.m_142416_(Button.m_253074_(Component.m_237113_("§e⚡ Extraer 64"), b -> {
            this.queryBox.m_94144_("SELECT * FROM infectados LIMIT 64");
            this.queryBox.m_93692_(true);
            this.queryBox.m_94201_();
        }).m_252987_(panelX + 6, top + 140, 80, 15).m_253136_());

        this.m_142416_(Button.m_253074_(Component.m_237113_("§6🔎 Filtrar %"), b -> {
            String tpl = "SELECT * FROM infectados WHERE name LIKE '%%'";
            this.queryBox.m_94144_(tpl);
            this.queryBox.m_93692_(true);
            this.queryBox.m_94196_(tpl.length() - 2);
        }).m_252987_(panelX + 89, top + 140, 80, 15).m_253136_());
    }

    @Override
    protected void m_181908_() {
        super.m_181908_();
        this.tickCounter++;
        if (this.tickCounter % 20 == 1 && this.f_97732_.getBlockEntity() != null) {
            SqlModPacketHandler.INSTANCE.sendToServer(new RequestNetworkStatusC2SPacket(this.f_97732_.getBlockEntity().m_58899_()));
        }
    }

    private void submitQuery() {
        String q = this.queryBox.m_94155_().trim();
        if (!q.isEmpty() && this.f_97732_.getBlockEntity() != null) {
            history.add("§c> §f" + q);
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
    public boolean m_6050_(double mouseX, double mouseY, double delta) {
        int left = (this.f_96543_ - this.f_97726_) / 2;
        int top = (this.f_96544_ - this.f_97727_) / 2;
        if (mouseX >= left && mouseX <= left + CONSOLE_W && mouseY >= top + 33 && mouseY <= top + GUI_H - 32) {
            int maxScroll = Math.max(0, history.size() - 15);
            int step = delta > 0 ? 1 : -1;
            scrollOffsetConsole = Math.max(0, Math.min(maxScroll, scrollOffsetConsole + step));
            return true;
        }
        return super.m_6050_(mouseX, mouseY, delta);
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
                if (cur.isEmpty()) {
                    this.queryBox.m_94144_("SELECT * FROM infectados WHERE name LIKE '%%'");
                    this.queryBox.m_94196_(this.queryBox.m_94155_().length() - 2);
                } else if (cur.equalsIgnoreCase("SELECT") || cur.equalsIgnoreCase("SELECT *")) {
                    this.queryBox.m_94144_("SELECT * FROM infectados ");
                    this.queryBox.m_94201_();
                } else {
                    int lastSpace = raw.lastIndexOf(' ');
                    String prefix = lastSpace == -1 ? "" : raw.substring(0, lastSpace + 1);
                    String lastWord = (lastSpace == -1 ? raw : raw.substring(lastSpace + 1)).toLowerCase();
                    if (!lastWord.isEmpty()) {
                        for (String cand : new String[]{
                                "SELECT", "FROM infectados", "WHERE", "ORDER BY", "GROUP BY", "LIMIT",
                                "SHOW ITEMS", "SCAN", "COUNT(*)", "LIKE", "nombre", "cantidad", "dano",
                                "durabilidad_pct", "rareza", "encantado", "apilable", "etiqueta", "mod",
                                "es_mineral", "es_arma", "es_armadura", "es_herramienta"
                        }) {
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
            if (keyCode == 265) {
                if (!queryHistory.isEmpty()) {
                    this.historyIndex = Math.min(this.historyIndex + 1, queryHistory.size() - 1);
                    this.queryBox.m_94144_(queryHistory.get(this.historyIndex));
                    this.queryBox.m_94201_();
                }
                return true;
            }
            if (keyCode == 264) {
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
    protected void m_7286_(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int left = (this.f_96543_ - this.f_97726_) / 2;
        int top = (this.f_96544_ - this.f_97727_) / 2;

        g.m_280509_(left, top, left + 330, top + 220, -15987700);
        g.m_280509_(left, top, left + 330, top + 14, -14024704);
        g.m_280509_(left + 1, top + 14, left + 329, top + 33, -15266530); // Toolbar backdrop
        g.m_280509_(left, top + 220 - 33, left + 330, top + 220, -15461336);
        drawBorder(g, left, top, 330, 220, -48060);
        g.m_280509_(left + 2, top + 220 - 34, left + 330 - 2, top + 220 - 33, -48060);
        g.m_280509_(left + 2, top + 14, left + 330 - 2, top + 15, -5627358);
        g.m_280509_(left + 2, top + 33, left + 330 - 2, top + 34, -8380380);

        int px = left + 330;
        g.m_280509_(px, top, px + 175, top + 220, -16249576);
        g.m_280509_(px + 1, top + 1, px + 175 - 1, top + 15, -14548975);
        g.m_280509_(px + 2, top + 15, px + 175 - 2, top + 16, -5636011);
        drawBorder(g, px, top, 175, 220, -65451);
        g.m_280509_(left + 330 - 1, top, left + 330, top + 220, -48060);
    }

    @Override
    public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(g);
        super.m_88315_(g, mouseX, mouseY, partialTick);
        this.m_280072_(g, mouseX, mouseY);
    }

    @Override
    protected void m_280003_(GuiGraphics g, int mouseX, int mouseY) {
        g.m_280056_(this.f_96547_, "§c◈ §c§lPortable §4Terminal §8v1.2", 4, 3, 16729156, false);
        String logHdr = "§7" + history.size() + " logs";
        int hdrW = this.f_96547_.m_92895_(Component.m_237113_(logHdr).getString());
        g.m_280056_(this.f_96547_, logHdr, 326 - hdrW, 3, 11184810, false);

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
            g.m_280509_(324, thumbY, 327, thumbY + thumbH, -48060);
        }

        g.m_280056_(this.f_96547_, "§7↑↓ hist · Tab · Shift+★ guardar favorito", 4, 210, 5592405, false);

        int px = 330;
        g.m_280056_(this.f_96547_, "§c§l☣ Red de Infección", px + 5, 3, 16711765, false);

        int py = 19;
        boolean hasKey = this.f_97732_.hasNetworkKey();
        if (hasKey && networkedChests > 0 && networkTimeLeft > 0L) {
            g.m_280509_(px + 2, py, px + 175 - 2, py + 26, 570438400);
            g.m_280509_(px + 2, py, px + 4, py + 26, -16711868);
            g.m_280056_(this.f_96547_, "§a● Red Activa", px + 7, py + 2, 5635925, false);
            int mins = (int) (networkTimeLeft / 60000L);
            int secs = (int) ((networkTimeLeft % 60000L) / 1000L);
            g.m_280056_(this.f_96547_, String.format("§eT- %02d:%02d", mins, secs), px + 7, py + 11, 16777045, false);

            // Visual Time Progress Bar (assumes 10 min = 600,000 ms reference)
            float pct = Math.max(0.0F, Math.min(1.0F, networkTimeLeft / 600000.0F));
            int barW = (int) (160 * pct);
            int barCol = pct > 0.5F ? 0xFF28E050 : (pct > 0.2F ? 0xFFFFAA00 : 0xFFFF3333);
            g.m_280509_(px + 7, py + 21, px + 167, py + 24, 0xFF222222);
            if (barW > 0) {
                g.m_280509_(px + 7, py + 21, px + 7 + barW, py + 24, barCol);
            }
            py += 29;

            g.m_280509_(px + 2, py, px + 175 - 2, py + 20, 452984831);
            g.m_280509_(px + 2, py, px + 4, py + 20, -16711800);
            g.m_280056_(this.f_96547_, "§bContenedores: §e" + networkedChests, px + 7, py + 6, 5636095, false);
            py += 23;
        } else if (hasKey) {
            g.m_280509_(px + 2, py, px + 175 - 2, py + 22, 587180544);
            g.m_280509_(px + 2, py, px + 4, py + 22, -22016);
            g.m_280056_(this.f_96547_, "§6● Red Expirada", px + 7, py + 2, 16755200, false);
            g.m_280056_(this.f_96547_, "§8Usa un nuevo catalizador", px + 7, py + 12, 7829367, false);
            py += 26;
        } else {
            g.m_280509_(px + 2, py, px + 175 - 2, py + 22, 587137024);
            g.m_280509_(px + 2, py, px + 4, py + 22, -48060);
            g.m_280056_(this.f_96547_, "§c● Sin Llave", px + 7, py + 2, 16733525, false);
            g.m_280056_(this.f_96547_, "§8Clic derecho con la llave", px + 7, py + 12, 7829367, false);
            py += 26;
        }

        g.m_280509_(px + 6, py, px + 175 - 6, py + 1, -13434829);
        py += 4;

        String dim = this.f_97732_.getOutputBoxDim();
        if (dim != null && !dim.isEmpty()) {
            g.m_280509_(px + 2, py, px + 175 - 2, py + 21, 436242687);
            g.m_280509_(px + 2, py, px + 4, py + 21, -16733441);
            g.m_280056_(this.f_96547_, "§b◈ Output Box", px + 7, py + 2, 5636095, false);
            String shortDim = dim.replace("minecraft:", "mc:");
            if (shortDim.length() > 20) shortDim = shortDim.substring(0, 18) + "..";
            g.m_280056_(this.f_96547_, "§7Dim: " + shortDim, px + 7, py + 12, 7829367, false);
        } else {
            g.m_280509_(px + 2, py, px + 175 - 2, py + 16, 587145762);
            g.m_280509_(px + 2, py, px + 4, py + 16, -48060);
            g.m_280056_(this.f_96547_, "§c◈ Sin Output Box", px + 7, py + 4, 16733525, false);
        }

        g.m_280509_(px + 2, 160, px + 175 - 2, 217, 184549375);
        g.m_280056_(this.f_96547_, "§7Clic llave: §avincular red", px + 5, 165, 5592405, false);
        g.m_280056_(this.f_96547_, "§7Shift+clic vacío: §eretirar llave", px + 5, 176, 5592405, false);
        g.m_280056_(this.f_96547_, "§7Linker: §bvincular Output Box", px + 5, 187, 5592405, false);
        g.m_280056_(this.f_96547_, "§8WHERE name LIKE '%ingot%'", px + 5, 202, 4473924, false);
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.m_280509_(x, y, x + w, y + 1, color);
        g.m_280509_(x, y + h - 1, x + w, y + h, color);
        g.m_280509_(x, y, x + 1, y + h, color);
        g.m_280509_(x + w - 1, y, x + w, y + h, color);
    }
}
