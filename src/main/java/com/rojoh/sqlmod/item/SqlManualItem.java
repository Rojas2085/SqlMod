package com.rojoh.sqlmod.item;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class SqlManualItem extends Item {

    public SqlManualItem(Properties properties) {
        super(properties.m_41487_(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.m_21120_(hand);
        if (!level.f_46443_) {
            ItemStack book = createManualBook();
            if (!player.m_150109_().m_36054_(book)) {
                player.m_36176_(book, false);
            }
            level.m_5594_(null, player.m_20183_(), SoundEvents.f_144243_, SoundSource.PLAYERS, 0.8F, 1.4F);
            player.m_5661_(Component.m_237113_("§a📖 Manual SQL Storage v1.2 generado en tu inventario."), true);
        }
        return InteractionResultHolder.m_19092_(held, level.m_5776_());
    }

    public static ItemStack createManualBook() {
        ItemStack book = new ItemStack(Items.f_42614_); // WRITTEN_BOOK
        CompoundTag tag = book.m_41784_();
        tag.m_128359_("title", "Manual SQL Storage v1.2");
        tag.m_128359_("author", "SQL Mod Docs");
        tag.m_128379_("resolved", true);

        ListTag pages = new ListTag();

        pages.add(makePage(
                "§2§lSQL STORAGE MOD\n§0Guía Rápida v1.2\n\n" +
                "§11.§0 Coloca una Terminal SQL y una Output Box.\n" +
                "§12.§0 Usa el Enlazador de Cofres (o Láser) para dar nombre a cada cofre.\n" +
                "§13.§0 Abre la Terminal y consulta o mueve ítems con SQL real!"
        ));

        pages.add(makePage(
                "§2§lCOMANDOS BASE\n\n" +
                "§1SHOW TABLES§0\nLista cofres vinculados.\n\n" +
                "§1SELECT * FROM <cofre>§0\nExtrae hacia Output Box.\n\n" +
                "§1INSERT INTO <dest> SELECT * FROM <orig>§0\nMueve ítems.\n\n" +
                "§1ROLLBACK§0\nDevuelve ítems."
        ));

        pages.add(makePage(
                "§2§lCOLUMNAS VIRTUALES\n\n" +
                "• §1nombre§0 / §1name§0\n" +
                "• §1cantidad§0 / §1count§0\n" +
                "• §1dano§0 / §1durabilidad_pct§0\n" +
                "• §1encantado§0 (true/false)\n" +
                "• §1es_mineral§0 (true/false)\n" +
                "• §1es_arma§0 / §1es_armadura§0\n" +
                "• §1es_herramienta§0\n" +
                "• §1mod§0 (minecraft, etc.)"
        ));

        pages.add(makePage(
                "§2§lFILTROS LIKE\n\n" +
                "Usa §1%§0 como comodín:\n\n" +
                "• '%ingot%' (lingotes)\n" +
                "• '%raw_%' (brutos)\n" +
                "• '%_ore%' (menas)\n" +
                "• '%sword%' (espadas)\n" +
                "• '%bone%' (huesos)\n" +
                "• '%diamond%' (diamante)"
        ));

        pages.add(makePage(
                "§2§lTRIGGERS Y OVERFLOW\n\n" +
                "§1CREATE TRIGGER§0 Auto\n" +
                "§1ON§0 Granja\n" +
                "§1WHERE§0 es_arma = true\n" +
                "§1DO§0 INSERT INTO Armeria\n\n" +
                "§6TIP OVERFLOW:§0\nVincula un cofre llamado §1overflow§0 o §1desborde§0 para evitar atascos!"
        ));

        pages.add(makePage(
                "§2§lBLOQUES v1.2\n\n" +
                "• §1Buzón Auditoría:§0\nImprime libros manifiesto al terminar cada lote.\n\n" +
                "• §1Sensor Capacidad:§0\nEmite Redstone 0-15 según llenado.\n\n" +
                "• §1Firewall SQL:§0\nProtege tus terminales en un radio de 32 bloques."
        ));

        tag.m_128365_("pages", pages);
        return book;
    }

    private static StringTag makePage(String text) {
        return StringTag.m_129297_(Component.Serializer.m_130703_(Component.m_237113_(text)));
    }

    @Override
    public void m_7373_(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.m_237113_("§aClic derecho: §7Genera el Manual SQL completo (6 páginas)"));
        tooltip.add(Component.m_237113_("§7Incluye sintaxis, columnas, patrones LIKE y triggers."));
    }
}
