package stellarstory.common.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;
import stellarstory.StellarStory;
import stellarstory.common.equipment.EquipSlotType;
import stellarstory.common.init.StellarCapabilities;
import stellarstory.network.NetworkHandler;
import stellarstory.network.packet.client.SyncPlayerCapabilityPacket;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(
        modid = StellarStory.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class PlayerCapability {
    public static final PlayerCapability PLAYER_CAPABILITY = new PlayerCapability();

    // ═══════════════════════════════════════════════════════════════
    // Инвентарь скафандра
    // ═══════════════════════════════════════════════════════════════

    private final ItemStackHandler suitInventory = new ItemStackHandler(EquipSlotType.values().length);

    public ItemStackHandler getSuitInventory() {
        return suitInventory;
    }

    /**
     * Получить предмет в слоте экипировки.
     */
    public ItemStack getSuitItem(EquipSlotType type) {
        return suitInventory.getStackInSlot(type.ordinal());
    }

    /**
     * Поместить предмет в слот экипировки.
     */
    public void setSuitItem(EquipSlotType type, ItemStack stack) {
        suitInventory.setStackInSlot(type.ordinal(), stack);
    }

    /**
     * Удалить предмет из слота.
     */
    public ItemStack removeSuitItem(EquipSlotType type, int count) {
        return suitInventory.extractItem(type.ordinal(), count, false);
    }

    /**
     * Обновить данные (вызывать при изменении).
     */
    public void setChanged() {
        // Данные сохраняются автоматически через capability
    }

    // ═══════════════════════════════════════════════════════════════
    // Разблокированные станции
    // ═══════════════════════════════════════════════════════════════

    /** Имена станций (Station#getName), открытые игроком. Сохраняются в NBT. */
    private final Set<String> unlockedStations = new HashSet<>();

    public boolean isStationUnlocked(String stationName) {
        return unlockedStations.contains(stationName);
    }

    /** @return true, если станция была открыта именно этим вызовом */
    public boolean unlockStation(String stationName) {
        return unlockedStations.add(stationName);
    }

    public void lockStation(String stationName) {
        unlockedStations.remove(stationName);
    }

    // ═══════════════════════════════════════════════════════════════
    // NBT
    // ═══════════════════════════════════════════════════════════════

    public Tag writeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("SuitInventory", suitInventory.serializeNBT());
        ListTag stations = new ListTag();
        for (String name : unlockedStations) {
            stations.add(StringTag.valueOf(name));
        }
        nbt.put("UnlockedStations", stations);
        return nbt;
    }

    public void readNBT(Tag tag) {
        CompoundTag nbt = (CompoundTag) tag;
        if (nbt.contains("SuitInventory")) {
            suitInventory.deserializeNBT(nbt.getCompound("SuitInventory"));
        }
        unlockedStations.clear();
        ListTag stations = nbt.getList("UnlockedStations", Tag.TAG_STRING);
        for (int i = 0; i < stations.size(); i++) {
            unlockedStations.add(stations.getString(i));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Синхронизация
    // ═══════════════════════════════════════════════════════════════

    public void syncPlayerCapability(Entity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            NetworkHandler.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SyncPlayerCapabilityPacket(this));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Capability Provider
    // ═══════════════════════════════════════════════════════════════

    @Mod.EventBusSubscriber
    private static class Provider implements ICapabilitySerializable<Tag> {
        private final PlayerCapability playerCapability = new PlayerCapability();
        private final LazyOptional<PlayerCapability> instance = LazyOptional.of(() -> playerCapability);

        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, "player_variables");

            if (!event.getCapabilities().containsKey(loc) && event.getObject() instanceof Player && !(event.getObject() instanceof FakePlayer)) {
                event.addCapability(loc, new Provider());
            }
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return cap == StellarCapabilities.PLAYER_CAPABILITY ? instance.cast() : LazyOptional.empty();
        }

        @Override
        public Tag serializeNBT() {
            return playerCapability.writeNBT();
        }

        @Override
        public void deserializeNBT(Tag nbt) {
            playerCapability.readNBT(nbt);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Event Handlers
    // ═══════════════════════════════════════════════════════════════

    @Mod.EventBusSubscriber
    public static class Handler {

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (!event.getEntity().level().isClientSide()) {
                event.getEntity().getCapability(StellarCapabilities.PLAYER_CAPABILITY, null)
                        .orElse(new PlayerCapability())
                        .syncPlayerCapability(event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
            if (!event.getEntity().level().isClientSide()) {
                event.getEntity().getCapability(StellarCapabilities.PLAYER_CAPABILITY, null)
                        .orElse(new PlayerCapability())
                        .syncPlayerCapability(event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (!event.getEntity().level().isClientSide()) {
                event.getEntity().getCapability(StellarCapabilities.PLAYER_CAPABILITY, null)
                        .orElse(new PlayerCapability())
                        .syncPlayerCapability(event.getEntity());
            }
        }

        @SubscribeEvent
        public static void clonePlayer(PlayerEvent.Clone event) {
            PlayerCapability original = event.getOriginal().getCapability(StellarCapabilities.PLAYER_CAPABILITY, null)
                    .orElse(new PlayerCapability());
            PlayerCapability clone = event.getEntity().getCapability(StellarCapabilities.PLAYER_CAPABILITY, null)
                    .orElse(new PlayerCapability());
            // Копируем инвентарь скафандра при возрождении
            clone.suitInventory.deserializeNBT(original.suitInventory.serializeNBT());
            // Разблокировки станций переживают смерть
            clone.unlockedStations.addAll(original.unlockedStations);
        }
    }
}
