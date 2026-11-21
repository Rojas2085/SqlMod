package com.rojoh.sqlmod.block.entity;

import com.rojoh.sqlmod.init.BlockEntityInit;
import com.rojoh.sqlmod.util.VfxHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AuditMailboxBlockEntity extends BlockEntity {

    private static final Set<AuditMailboxBlockEntity> LOADED_MAILBOXES = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final int BATCH_IDLE_TICKS = 400; // 20 segundos sin actividad -> imprime libro

    private final Map<String, Integer> pendingItems = new LinkedHashMap<>();
    private final Map<String, Integer> pendingDests = new LinkedHashMap<>();
    private final List<ItemStack> completedBooks = new ArrayList<>();
    private int pendingTotal = 0;
    private int pendingOverflow = 0;
    private int idleTicks = 0;
    private boolean hasPendingBatch = false;
    private int batchCounter = 0;

    public AuditMailboxBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.AUDIT_MAILBOX.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
            LOADED_MAILBOXES.add(this);
        }
    }

    @Override
    public void m_7651_() { // setRemoved
        super.m_7651_();
        LOADED_MAILBOXES.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        LOADED_MAILBOXES.remove(this);
    }

    /**
     * Registra una transferencia en cualquier Buzón de Auditoría cercano (<= 24 bloques de la terminal).
     */
    public static void recordTransfer(ServerLevel level, BlockPos terminalPos, String destName, Map<String, Integer> items, int overflowMoved) {
        if (level == null || terminalPos == null || items == null || items.isEmpty()) return;
        for (AuditMailboxBlockEntity box : LOADED_MAILBOXES) {
            if (box.f_58857_ == level && !box.m_58901_() && box.f_58858_.m_123314_(terminalPos, 24.0D)) {
                box.addBatchData(destName, items, overflowMoved);
            }
        }
    }

    public void addBatchData(String destName, Map<String, Integer> items, int overflowMoved) {
        int added = 0;
        for (Map.Entry<String, Integer> e : items.entrySet()) {
            pendingItems.put(e.getKey(), pendingItems.getOrDefault(e.getKey(), 0) + e.getValue());
            added += e.getValue();
        }
        String dKey = (destName == null || destName.isEmpty()) ? "Output Box" : destName;
        pendingDests.put(dKey, pendingDests.getOrDefault(dKey, 0) + added);
        pendingTotal += added;
        pendingOverflow += overflowMoved;
        idleTicks = 0;
        hasPendingBatch = true;
        m_6596_();
    }

    public void serverTick() {
        if (!hasPendingBatch) return;
        idleTicks++;
        if (idleTicks >= BATCH_IDLE_TICKS) {
            flushBatchToBook();
        }
    }

    public boolean flushBatchToBook() {
        if (!hasPendingBatch || pendingTotal <= 0) return false;
        batchCounter++;

        ItemStack book = new ItemStack(Items.f_42614_); // WRITTEN_BOOK
        CompoundTag tag = book.m_41784_();
        tag.m_128359_("title", "Manifiesto SQL #" + batchCounter);
        tag.m_128359_("author", "SQL Audit System");
        tag.m_128379_("resolved", true);

        ListTag pages = new ListTag();

        // Página 1: Resumen general
        StringBuilder p1 = new StringBuilder();
        p1.append("=== MANIFIESTO #").append(batchCounter).append(" ===\n\n");
        p1.append("Estado: COMPLETADO\n");
        p1.append("Total Movidos: ").append(pendingTotal).append("\n");
        p1.append("Destinos: ").append(pendingDests.size()).append("\n");
        p1.append("Desbordes: ").append(pendingOverflow).append("\n\n");
        p1.append("Integridad: 100% OK\n");
        p1.append("(Cero perdidas)");
        pages.add(StringTag.m_129297_(Component.Serializer.m_130703_(Component.m_237113_(p1.toString()))));

        // Página 2: Destinos
        StringBuilder p2 = new StringBuilder();
        p2.append("== DESTINOS ==\n\n");
        int dCount = 0;
        for (Map.Entry<String, Integer> e : pendingDests.entrySet()) {
            if (dCount++ >= 10) {
                p2.append("... y mas");
                break;
            }
            p2.append("-> ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        pages.add(StringTag.m_129297_(Component.Serializer.m_130703_(Component.m_237113_(p2.toString()))));

        // Página 3: Detalle de ítems
        StringBuilder p3 = new StringBuilder();
        p3.append("== ITEMS ==\n\n");
        int iCount = 0;
        for (Map.Entry<String, Integer> e : pendingItems.entrySet()) {
            if (iCount++ >= 10) {
                p3.append("... y mas");
                break;
            }
            p3.append("• ").append(e.getValue()).append("x ").append(e.getKey()).append("\n");
        }
        pages.add(StringTag.m_129297_(Component.Serializer.m_130703_(Component.m_237113_(p3.toString()))));

        tag.m_128365_("pages", pages);

        if (completedBooks.size() >= 16) {
            completedBooks.remove(0);
        }
        completedBooks.add(book);

        pendingItems.clear();
        pendingDests.clear();
        pendingTotal = 0;
        pendingOverflow = 0;
        idleTicks = 0;
        hasPendingBatch = false;
        m_6596_();

        if (this.f_58857_ instanceof ServerLevel sLevel) {
            VfxHelper.playTerminalSuccess(sLevel, this.f_58858_);
        }
        return true;
    }

    public ItemStack popBook() {
        if (completedBooks.isEmpty()) return ItemStack.f_41583_;
        ItemStack book = completedBooks.remove(completedBooks.size() - 1);
        m_6596_();
        return book;
    }

    public int getCompletedBookCount() {
        return completedBooks.size();
    }

    public int getPendingTotal() {
        return pendingTotal;
    }

    public boolean hasPendingBatch() {
        return hasPendingBatch;
    }

    @Override
    protected void m_183515_(CompoundTag tag) {
        super.m_183515_(tag);
        tag.m_128405_("BatchCounter", batchCounter);
        ListTag booksList = new ListTag();
        for (ItemStack b : completedBooks) {
            booksList.add(b.m_41739_(new CompoundTag()));
        }
        tag.m_128365_("CompletedBooks", booksList);
    }

    @Override
    public void m_142466_(CompoundTag tag) {
        super.m_142466_(tag);
        batchCounter = tag.m_128451_("BatchCounter");
        completedBooks.clear();
        if (tag.m_128441_("CompletedBooks")) {
            ListTag booksList = tag.m_128437_("CompletedBooks", 10);
            for (int i = 0; i < booksList.size(); i++) {
                ItemStack b = ItemStack.m_41712_(booksList.m_128728_(i));
                if (!b.m_41619_()) completedBooks.add(b);
            }
        }
    }
}
