package com.rojoh.sqlmod.block.entity;

import com.rojoh.sqlmod.init.BlockEntityInit;
import com.rojoh.sqlmod.sql.DatabaseManager;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

public class RecicladorBlockEntity extends BlockEntity {

    private UUID ownerUUID;
    private String ownerName;
    private long puntosCache;
    private long puntosPendientes;
    private int tickCounter;

    public static final int DEFAULT_VALOR = 2;
    public static final Map<Item, Integer> VALORES = new HashMap<>();
    public static final Map<Item, Long> CATALOGO = new HashMap<>();

    private final IItemHandler hopperHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.f_41583_; // EMPTY
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.m_41619_() || stack.m_41720_() == Items.f_41852_) { // AIR
                return stack;
            }

            int valor = getValor(stack.m_41720_());
            if (!simulate) {
                long pts = (long) valor * stack.m_41613_();
                puntosCache += pts;
                puntosPendientes += pts;
                m_6596_(); // setChanged
            }
            return ItemStack.f_41583_; // Consumed completely
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.f_41583_;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !stack.m_41619_() && stack.m_41720_() != Items.f_41852_;
        }
    };

    private final LazyOptional<IItemHandler> lazyHandler = LazyOptional.of(() -> hopperHandler);

    public RecicladorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.RECICLADOR.get(), pos, state);
    }

    public static int getValor(Item item) {
        if (item == null || item == Items.f_41852_) return 0;

        // 1. Verificar valores explícitos mapeados
        Integer exp = VALORES.get(item);
        if (exp != null) return exp;

        // 2. Cálculo inteligente para minerales y recursos por registry name
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        if (rl != null) {
            String path = rl.m_135815_().toLowerCase(Locale.ROOT);

            // Netherite
            if (path.equals("netherite_block")) return 18000;
            if (path.equals("netherite_ingot")) return 2000;
            if (path.equals("ancient_debris")) return 600;
            if (path.equals("netherite_scrap")) return 500;

            // Diamante
            if (path.equals("diamond_block")) return 2250;
            if (path.contains("diamond_ore")) return 300;
            if (path.equals("diamond")) return 250;

            // Esmeralda
            if (path.equals("emerald_block")) return 1350;
            if (path.contains("emerald_ore")) return 200;
            if (path.equals("emerald")) return 150;

            // Oro
            if (path.equals("gold_block")) return 360;
            if (path.equals("raw_gold_block")) return 315;
            if (path.contains("gold_ore")) return 50;
            if (path.equals("gold_ingot")) return 40;
            if (path.equals("raw_gold")) return 35;
            if (path.equals("gold_nugget")) return 4;

            // Hierro
            if (path.equals("iron_block")) return 225;
            if (path.equals("raw_iron_block")) return 180;
            if (path.contains("iron_ore")) return 30;
            if (path.equals("iron_ingot")) return 25;
            if (path.equals("raw_iron")) return 20;
            if (path.equals("iron_nugget")) return 2;

            // Cobre
            if (path.equals("copper_block")) return 90;
            if (path.equals("raw_copper_block")) return 72;
            if (path.contains("copper_ore")) return 12;
            if (path.equals("copper_ingot")) return 10;
            if (path.equals("raw_copper")) return 8;

            // Lapis y Redstone
            if (path.equals("lapis_block")) return 135;
            if (path.contains("lapis_ore")) return 20;
            if (path.equals("lapis_lazuli")) return 15;
            if (path.equals("redstone_block")) return 108;
            if (path.contains("redstone_ore")) return 15;
            if (path.equals("redstone")) return 12;

            // Amatista y Cuarzo
            if (path.equals("amethyst_cluster")) return 40;
            if (path.equals("amethyst_block")) return 72;
            if (path.equals("amethyst_shard")) return 18;
            if (path.contains("quartz_ore")) return 20;
            if (path.equals("quartz_block")) return 60;
            if (path.equals("quartz")) return 15;

            // Carbón
            if (path.equals("coal_block")) return 54;
            if (path.contains("coal_ore")) return 8;
            if (path.equals("coal") || path.equals("charcoal")) return 6;

            // Ítems mágicos / mobs especiales
            if (path.equals("nether_star")) return 5000;
            if (path.equals("totem_of_undying")) return 800;
            if (path.equals("wither_skeleton_skull")) return 500;
            if (path.equals("dragon_breath")) return 60;
            if (path.equals("ghast_tear")) return 50;
            if (path.equals("ender_pearl") || path.equals("ender_eye")) return 40;
            if (path.equals("blaze_rod")) return 30;
            if (path.equals("shulker_shell")) return 50;

            // Detección genérica de otros mods (ore / raw / ingot / gem)
            if (path.contains("_ore")) return 20;
            if (path.startsWith("raw_")) return 15;
            if (path.endsWith("_ingot")) return 25;
            if (path.endsWith("_gem")) return 50;
        }

        return DEFAULT_VALOR;
    }

    public static long getCosto(Item item) {
        if (CATALOGO.containsKey(item)) {
            return CATALOGO.get(item);
        }
        return 1000L;
    }

    public static boolean isItemValidoParaTienda(Item item) {
        if (item == Items.f_41852_) return false;
        if (item instanceof SpawnEggItem) return false;
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.m_40614_();
            BlockState state = block.m_49966_();
            if (state.m_60800_(null, BlockPos.f_121853_) < 0.0F) {
                return false; // Bloques indestructibles
            }
        }
        return true;
    }

    public void serverTick() {
        if (puntosPendientes != 0) {
            tickCounter++;
            if (tickCounter >= 20) {
                long pending = puntosPendientes;
                puntosPendientes = 0;
                tickCounter = 0;

                BlockPos pos = this.f_58858_; // worldPosition
                String dim = this.f_58857_.m_46472_().m_135782_().toString();
                String ownerStr = ownerUUID != null ? ownerUUID.toString() : "unknown";

                DatabaseManager.sumarPuntosAsync(pos.m_123341_(), pos.m_123342_(), pos.m_123343_(), dim, ownerStr, pending);
            }
        }
    }

    public boolean gastarPuntos(long costo) {
        if (this.puntosCache >= costo) {
            this.puntosCache -= costo;
            this.puntosPendientes -= costo;
            m_6596_(); // setChanged
            return true;
        }
        return false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.refrescarPuntos();
    }

    public void setOwner(UUID uuid, String name) {
        this.ownerUUID = uuid;
        this.ownerName = name;
        m_6596_();
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isOwner(UUID playerUUID) {
        return ownerUUID != null && ownerUUID.equals(playerUUID);
    }

    public long getPuntosCache() {
        return puntosCache;
    }

    public void refrescarPuntos() {
        if (this.f_58857_ == null || this.f_58857_.f_46443_) return; // level isClientSide
        BlockPos pos = this.f_58858_;
        String dim = this.f_58857_.m_46472_().m_135782_().toString();

        CompletableFuture.supplyAsync(() -> {
            return DatabaseManager.obtenerPuntos(pos.m_123341_(), pos.m_123342_(), pos.m_123343_(), dim);
        }).thenAccept(pts -> {
            if (this.f_58857_ != null && this.f_58857_.m_7654_() != null) {
                this.f_58857_.m_7654_().execute(() -> {
                    // Nunca sobrescribir puntosCache con 0 si ya teníamos puntos guardados en NBT
                    if (pts > this.puntosCache) {
                        this.puntosCache = pts;
                        m_6596_();
                    } else if (this.puntosCache > pts) {
                        // Sincronizar el estado NBT hacia DatabaseManager
                        String ownerStr = ownerUUID != null ? ownerUUID.toString() : "unknown";
                        DatabaseManager.sumarPuntosAsync(pos.m_123341_(), pos.m_123342_(), pos.m_123343_(), dim, ownerStr, this.puntosCache - pts);
                    }
                });
            }
        });
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyHandler.invalidate();
    }

    @Override
    protected void m_183515_(CompoundTag tag) { // saveAdditional
        super.m_183515_(tag);
        if (this.ownerUUID != null) {
            tag.m_128362_("OwnerUUID", this.ownerUUID);
        }
        if (this.ownerName != null) {
            tag.m_128359_("OwnerName", this.ownerName);
        }
        tag.m_128356_("PuntosCache", this.puntosCache);
        tag.m_128356_("PuntosPendientes", this.puntosPendientes);
    }

    @Override
    public void m_142466_(CompoundTag tag) { // load
        super.m_142466_(tag);
        if (tag.m_128403_("OwnerUUID")) {
            this.ownerUUID = tag.m_128342_("OwnerUUID");
        }
        if (tag.m_128441_("OwnerName")) {
            this.ownerName = tag.m_128461_("OwnerName");
        }
        this.puntosCache = tag.m_128454_("PuntosCache");
        if (tag.m_128441_("PuntosPendientes")) {
            this.puntosPendientes = tag.m_128454_("PuntosPendientes");
        }
    }

    @Override
    public CompoundTag m_5995_() { // getUpdateTag
        CompoundTag tag = super.m_5995_();
        m_183515_(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> m_58483_() { // getUpdatePacket
        return ClientboundBlockEntityDataPacket.m_195640_(this);
    }

    public void setPuntosFromServer(long puntos) {
        this.puntosCache = puntos;
    }

    static {
        // Materiales básicos de 1 punto (originales)
        VALORES.put(Items.f_42594_, 1);
        VALORES.put(Items.f_151035_, 1);
        VALORES.put(Items.f_42329_, 1);
        VALORES.put(Items.f_41832_, 1);
        VALORES.put(Items.f_41830_, 1);
        VALORES.put(Items.f_42048_, 1);

        // Catálogo base de compras rápidas en tienda
        CATALOGO.put(Items.f_42416_, 50L);   // raw_copper
        CATALOGO.put(Items.f_42417_, 150L);  // netherite_ingot
        CATALOGO.put(Items.f_42415_, 1000L); // emerald
        CATALOGO.put(Items.f_42616_, 600L);  // lapis_lazuli
        CATALOGO.put(Items.f_42418_, 5000L); // netherite_scrap
        CATALOGO.put(Items.f_42584_, 80L);   // blaze_rod
    }
}
