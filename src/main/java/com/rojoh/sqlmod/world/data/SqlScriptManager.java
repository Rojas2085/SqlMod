package com.rojoh.sqlmod.world.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class SqlScriptManager extends SavedData {
    private static final String DATA_NAME = "sqlmod_scripts";
    private final Map<BlockPos, Map<String, String>> macros = new HashMap<>();
    private final Map<BlockPos, Map<String, String>> views = new HashMap<>();

    public SqlScriptManager() {}

    public static SqlScriptManager get(ServerLevel level) {
        return level.m_7654_().m_129783_().m_8895_().m_164861_(
                SqlScriptManager::load,
                SqlScriptManager::new,
                DATA_NAME
        );
    }

    public static SqlScriptManager load(CompoundTag tag) {
        SqlScriptManager manager = new SqlScriptManager();
        if (tag.m_128441_("Macros")) {
            ListTag macroList = tag.m_128437_("Macros", 10);
            for (int i = 0; i < macroList.size(); i++) {
                CompoundTag entry = macroList.m_128728_(i);
                BlockPos pos = new BlockPos(entry.m_128451_("X"), entry.m_128451_("Y"), entry.m_128451_("Z"));
                String name = entry.m_128461_("Name");
                String script = entry.m_128461_("Script");
                manager.macros.computeIfAbsent(pos, k -> new LinkedHashMap<>()).put(name, script);
            }
        }
        if (tag.m_128441_("Views")) {
            ListTag viewList = tag.m_128437_("Views", 10);
            for (int i = 0; i < viewList.size(); i++) {
                CompoundTag entry = viewList.m_128728_(i);
                BlockPos pos = new BlockPos(entry.m_128451_("X"), entry.m_128451_("Y"), entry.m_128451_("Z"));
                String name = entry.m_128461_("Name");
                String script = entry.m_128461_("Script");
                manager.views.computeIfAbsent(pos, k -> new LinkedHashMap<>()).put(name, script);
            }
        }
        return manager;
    }

    @Override
    public CompoundTag m_7176_(CompoundTag tag) {
        ListTag macroList = new ListTag();
        for (Map.Entry<BlockPos, Map<String, String>> e : macros.entrySet()) {
            BlockPos pos = e.getKey();
            for (Map.Entry<String, String> scriptEntry : e.getValue().entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.m_128405_("X", pos.m_123341_());
                entry.m_128405_("Y", pos.m_123342_());
                entry.m_128405_("Z", pos.m_123343_());
                entry.m_128359_("Name", scriptEntry.getKey());
                entry.m_128359_("Script", scriptEntry.getValue());
                macroList.add(entry);
            }
        }
        tag.m_128365_("Macros", macroList);

        ListTag viewList = new ListTag();
        for (Map.Entry<BlockPos, Map<String, String>> e : views.entrySet()) {
            BlockPos pos = e.getKey();
            for (Map.Entry<String, String> scriptEntry : e.getValue().entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.m_128405_("X", pos.m_123341_());
                entry.m_128405_("Y", pos.m_123342_());
                entry.m_128405_("Z", pos.m_123343_());
                entry.m_128359_("Name", scriptEntry.getKey());
                entry.m_128359_("Script", scriptEntry.getValue());
                viewList.add(entry);
            }
        }
        tag.m_128365_("Views", viewList);
        return tag;
    }

    public void putMacro(BlockPos pos, String name, String script) {
        macros.computeIfAbsent(pos, k -> new LinkedHashMap<>()).put(name, script);
        m_77762_();
    }

    public Map<String, String> getMacros(BlockPos pos) {
        return macros.getOrDefault(pos, Collections.emptyMap());
    }

    public boolean removeMacro(BlockPos pos, String name) {
        Map<String, String> map = macros.get(pos);
        if (map != null && map.remove(name) != null) {
            m_77762_();
            return true;
        }
        return false;
    }

    public void putView(BlockPos pos, String name, String script) {
        views.computeIfAbsent(pos, k -> new LinkedHashMap<>()).put(name, script);
        m_77762_();
    }

    public Map<String, String> getViews(BlockPos pos) {
        return views.getOrDefault(pos, Collections.emptyMap());
    }

    public boolean removeView(BlockPos pos, String name) {
        Map<String, String> map = views.get(pos);
        if (map != null && map.remove(name) != null) {
            m_77762_();
            return true;
        }
        return false;
    }
}
