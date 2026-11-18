package com.rojoh.sqlmod.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeTabInit {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.f_279569_, "sqlmod");

    public static final RegistryObject<CreativeModeTab> SQL_TAB = CREATIVE_MODE_TABS.register("sql_tab",
            () -> CreativeModeTab.builder()
                    .m_257737_(() -> new ItemStack(ItemInit.SQL_TERMINAL.get()))
                    .m_257941_(Component.m_237115_("creativetab.sql_tab"))
                    .m_257501_((parameters, output) -> {
                        output.m_246326_(ItemInit.SQL_TERMINAL.get());
                        output.m_246326_(ItemInit.SQL_OUTPUT_BOX.get());
                        output.m_246326_(ItemInit.PORTABLE_TERMINAL.get());
                        output.m_246326_(ItemInit.WIRELESS_TERMINAL.get());
                        output.m_246326_(ItemInit.CHEST_LINKER.get());
                        output.m_246326_(ItemInit.LASER_CHEST_LINKER.get());
                        output.m_246326_(ItemInit.INFECTION_CATALYST.get());
                        output.m_246326_(ItemInit.INFECTION_RELAY.get());
                        output.m_246326_(ItemInit.INFECTION_PURIFIER.get());
                        output.m_246326_(ItemInit.NETWORK_KEY.get());
                        output.m_246326_(ItemInit.NETWORK_KEY_RING.get());
                        output.m_246326_(ItemInit.NETWORK_VISOR.get());
                        output.m_246326_(ItemInit.SQL_INSPECTOR.get());
                        output.m_246326_(ItemInit.QUERY_CARD.get());
                        output.m_246326_(ItemInit.RECICLADOR.get());
                    })
                    .m_257652_());
}
