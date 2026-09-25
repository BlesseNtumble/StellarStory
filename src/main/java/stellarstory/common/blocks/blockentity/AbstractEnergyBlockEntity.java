package stellarstory.common.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import stellarstory.api.energy.EnergyConfig;
import stellarstory.api.transfer.ISideConfigurable;
import stellarstory.api.transfer.SideConfig;
import stellarstory.api.transfer.SideMode;
import stellarstory.api.transfer.SlotRole;
import stellarstory.api.transfer.TransferKind;
import stellarstory.common.transfer.SidedItemHandler;

/**
 * Базовый BlockEntity механизма: энергия (Forge Energy) + инвентарь + настраиваемые роли сторон.
 *
 * Ограничители сторон (SidedEnergy / SidedItemHandler) читают SideConfig в момент обращения,
 * поэтому кэшированные LazyOptional переживают смену настроек игроком.
 */
public abstract class AbstractEnergyBlockEntity extends BlockEntity
        implements IEnergyTickedBlockEntity, ISideConfigurable {

    /** Direction.values() клонирует массив при каждом вызове — держим один раз. */
    protected static final Direction[] DIRS = Direction.values();

    /** Индекс для null-стороны (внутренний доступ). */
    private static final int NULL_SIDE_INDEX = 6;

    protected final EnergyConfig energyConfig;
    protected final SideConfig sideConfig;
    protected final SlotRole[] slotRoles;
    protected final ItemStackHandler inventory;

    protected int energy = 0;

    @SuppressWarnings("unchecked")
    private final LazyOptional<IEnergyStorage>[] energyCaps = new LazyOptional[7];
    @SuppressWarnings("unchecked")
    private final LazyOptional<IItemHandler>[] itemCaps = new LazyOptional[7];

    protected AbstractEnergyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                        EnergyConfig config, SideConfig defaultSides, SlotRole[] slotRoles) {
        super(type, pos, state);
        this.energyConfig = config;
        this.sideConfig = defaultSides;
        this.slotRoles = slotRoles;
        this.inventory = new ItemStackHandler(slotRoles.length) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                if (!slotRoles[slot].insertable) {
                    return true;
                }
                return AbstractEnergyBlockEntity.this.isItemValidForSlot(slot, stack);
            }
        };
    }

    // ═══════════════════════════════════════════════════════════════
    // Энергия
    // ═══════════════════════════════════════════════════════════════

    public int getEnergy() {
        return energy;
    }

    public int getMaxEnergy() {
        return energyConfig.capacity();
    }

    /** Списать энергию на работу механизма (внутренний расход, мимо capability). */
    protected boolean consumeEnergy(int amount) {
        if (energy < amount) {
            return false;
        }
        energy -= amount;
        setChanged();
        return true;
    }

    /** Начислить энергию изнутри (генерация). */
    protected void produceEnergy(int amount) {
        int added = Math.min(amount, energyConfig.capacity() - energy);
        if (added > 0) {
            energy += added;
            setChanged();
        }
    }

    protected boolean canReceiveOnFace(@Nullable Direction side) {
        return energyConfig.maxReceive() > 0 && sideConfig.canReceive(TransferKind.ENERGY, side);
    }

    protected boolean canExtractOnFace(@Nullable Direction side) {
        return energyConfig.maxExtract() > 0 && sideConfig.canExtract(TransferKind.ENERGY, side);
    }

    private int receiveInternal(int max, boolean simulate) {
        int accepted = Math.min(energyConfig.capacity() - energy, Math.min(energyConfig.maxReceive(), max));
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            energy += accepted;
            setChanged();
        }
        return accepted;
    }

    private int extractInternal(int max, boolean simulate) {
        int extracted = Math.min(energy, Math.min(energyConfig.maxExtract(), max));
        if (extracted <= 0) {
            return 0;
        }
        if (!simulate) {
            energy -= extracted;
            setChanged();
        }
        return extracted;
    }

    /** Сколько энергия готова отдать прямо сейчас (для simulate-опроса сетью). */
    public int extractableNow() {
        return Math.min(energy, energyConfig.maxExtract());
    }

    /** Сколько машина готова принять прямо сейчас. */
    public int receivableNow() {
        return Math.min(energyConfig.capacity() - energy, energyConfig.maxReceive());
    }

    /**
     * Приём энергии сетью/соседом. Проверяет роль грани так же, как capability-ограничитель,
     * иначе сеть могла бы залить энергию в DISABLED/OUTPUT-сторону.
     *
     * @param side грань, через которую пришёл запрос
     * @return реально принятое количество
     */
    public int receiveEnergyFromNetwork(@Nullable Direction side, int maxReceive, boolean simulate) {
        return canReceiveOnFace(side) ? receiveInternal(maxReceive, simulate) : 0;
    }

    /** Изъятие энергии сетью/соседом. Проверка роли грани обязательна, см. receiveEnergyFromNetwork. */
    public int extractEnergyFromNetwork(@Nullable Direction side, int maxExtract, boolean simulate) {
        return canExtractOnFace(side) ? extractInternal(maxExtract, simulate) : 0;
    }

    // ═══════════════════════════════════════════════════════════════
    // Инвентарь
    // ═══════════════════════════════════════════════════════════════

    public ItemStackHandler getInventory() {
        return inventory;
    }

    /** Внутренний доступ без ограничений по стороне. */
    public IItemHandler getInternalInventory() {
        return inventory;
    }

    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    protected ItemStack stackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    // ═══════════════════════════════════════════════════════════════
    // Ручная загрузка/выгрузка (до GUI)
    // ═══════════════════════════════════════════════════════════════

    /** Есть ли слот, в который игрок может что-то положить (для предсказания на клиенте). */
    public boolean hasInsertableSlot() {
        for (SlotRole role : slotRoles) {
            if (role.insertable) {
                return true;
            }
        }
        return false;
    }

    /**
     * ПКМ с предметом в руке: вставить стак в первый вставляемый слот.
     * Вызывать только на сервере. @return true, если что-то реально вставлено.
     */
    public boolean playerInsert(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < slotRoles.length; slot++) {
            if (!slotRoles[slot].insertable) {
                continue;
            }
            ItemStack rest = inventory.insertItem(slot, held, false);
            if (rest.getCount() != held.getCount()) {
                player.setItemInHand(hand, rest);
                return true;
            }
        }
        return false;
    }

    /**
     * Shift+ПКМ пустой рукой: забрать стак из первого извлекаемого слота.
     * Вызывать только на сервере. @return true, если что-то реально выдано.
     */
    public boolean playerExtract(Player player) {
        for (int slot = 0; slot < slotRoles.length; slot++) {
            if (!slotRoles[slot].extractable) {
                continue;
            }
            ItemStack extracted = inventory.extractItem(slot, 64, false);
            if (!extracted.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(player, extracted);
                return true;
            }
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════════
    // Capability
    // ═══════════════════════════════════════════════════════════════

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!isRemoved()) {
            if (cap == ForgeCapabilities.ENERGY) {
                return energyCap(side).cast();
            }
            if (cap == ForgeCapabilities.ITEM_HANDLER && inventory.getSlots() > 0) {
                return itemCap(side).cast();
            }
        }
        return super.getCapability(cap, side);
    }

    private LazyOptional<IEnergyStorage> energyCap(@Nullable Direction side) {
        int index = side == null ? NULL_SIDE_INDEX : side.ordinal();
        LazyOptional<IEnergyStorage> cached = energyCaps[index];
        if (cached == null) {
            cached = LazyOptional.of(() -> new SidedEnergy(side));
            energyCaps[index] = cached;
        }
        return cached;
    }

    private LazyOptional<IItemHandler> itemCap(@Nullable Direction side) {
        int index = side == null ? NULL_SIDE_INDEX : side.ordinal();
        LazyOptional<IItemHandler> cached = itemCaps[index];
        if (cached == null) {
            // null-сторона = внутренний доступ, без ограничений
            if (side == null) {
                cached = LazyOptional.of(() -> inventory);
            } else {
                SidedItemHandler handler = new SidedItemHandler(inventory, side, sideConfig, slotRoles);
                cached = LazyOptional.of(() -> handler);
            }
            itemCaps[index] = cached;
        }
        return cached;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (int i = 0; i < energyCaps.length; i++) {
            if (energyCaps[i] != null) {
                energyCaps[i].invalidate();
                energyCaps[i] = null;
            }
            if (itemCaps[i] != null) {
                itemCaps[i].invalidate();
                itemCaps[i] = null;
            }
        }
    }

    /** Ограничитель энергии по стороне поверх нашей ячейки. */
    private final class SidedEnergy implements IEnergyStorage {

        @Nullable
        private final Direction side;

        private SidedEnergy(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return canReceiveOnFace(side) ? receiveInternal(maxReceive, simulate) : 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return canExtractOnFace(side) ? extractInternal(maxExtract, simulate) : 0;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return energyConfig.capacity();
        }

        @Override
        public boolean canExtract() {
            return canExtractOnFace(side);
        }

        @Override
        public boolean canReceive() {
            return canReceiveOnFace(side);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Настройка сторон
    // ═══════════════════════════════════════════════════════════════

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public boolean setSideMode(TransferKind kind, Direction side, SideMode mode) {
        if (side == null || sideConfig.get(kind, side) == mode) {
            return false;
        }
        sideConfig.set(kind, side, mode);
        onSideConfigChanged();
        return true;
    }

    @Override
    public SideMode cycleSideMode(TransferKind kind, Direction side) {
        if (side == null) {
            return SideMode.DISABLED;
        }
        SideMode next = sideConfig.get(kind, side).next();
        sideConfig.set(kind, side, next);
        onSideConfigChanged();
        return next;
    }

    /** Пересобрать карты видимых слотов и обновить блок у клиента. */
    protected void onSideConfigChanged() {
        for (LazyOptional<IItemHandler> cap : itemCaps) {
            if (cap != null) {
                cap.ifPresent(handler -> {
                    if (handler instanceof SidedItemHandler sided) {
                        sided.refresh();
                    }
                });
            }
        }
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // NBT + синхронизация
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy);
        sideConfig.save(tag);
        if (inventory.getSlots() > 0) {
            tag.put("Inventory", inventory.serializeNBT());
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        energy = Math.max(0, Math.min(energyConfig.capacity(), tag.getInt("Energy")));
        sideConfig.load(tag);
        if (inventory.getSlots() > 0 && tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        writeSyncData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag) {
        readSyncData(tag);
    }

    /** В клиентский тег идёт только то, что нужно для отрисовки — не весь инвентарь. */
    protected void writeSyncData(CompoundTag tag) {
        tag.putInt("Energy", energy);
        sideConfig.save(tag);
    }

    protected void readSyncData(CompoundTag tag) {
        if (tag.contains("Energy")) {
            energy = Math.max(0, Math.min(energyConfig.capacity(), tag.getInt("Energy")));
        }
        sideConfig.load(tag);
    }
}
