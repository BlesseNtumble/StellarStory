package stellarstory.common.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import stellarstory.api.energy.EnergyConfig;
import stellarstory.api.transfer.ISideConfigurable;
import stellarstory.api.transfer.SideConfig;
import stellarstory.api.transfer.SideMode;
import stellarstory.api.transfer.TransferKind;
import stellarstory.common.menu.MachineMenu;
import stellarstory.common.transfer.SidedFluidHandler;
import stellarstory.common.transfer.SidedItemHandler;
import stellarstory.common.transfer.network.PipeNetworkManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Базовый BlockEntity механизма: энергия (Forge Energy) + инвентарь + жидкостные баки
 * + настраиваемые роли сторон для ENERGY / ITEM / FLUID.
 *
 * Ограничители сторон (SidedEnergy / SidedItemHandler / SidedFluidHandler) читают
 * SideConfig в момент обращения, поэтому кэшированные LazyOptional переживают
 * смену настроек игроком — инвалидация не нужна.
 */
public abstract class AbstractEnergyBlockEntity extends BlockEntity
        implements IEnergyTickedBlockEntity, ISideConfigurable, MenuProvider {

    /** Direction.values() клонирует массив при каждом вызове — держим один раз. */
    protected static final Direction[] DIRS = Direction.values();

    /** Индекс для null-стороны (внутренний доступ). */
    private static final int NULL_SIDE_INDEX = 6;

    /** Квадрат дистанции ванильного stillValid (8 блоков). */
    private static final double MAX_USE_DISTANCE_SQR = 64.0D;

    protected final EnergyConfig energyConfig;
    protected final SideConfig sideConfig;
    protected final ItemStackHandler inventory;
    protected final List<FluidTank> tanks = new ArrayList<>();

    protected int energy = 0;

    @SuppressWarnings("unchecked")
    private final LazyOptional<IEnergyStorage>[] energyCaps = new LazyOptional[7];
    @SuppressWarnings("unchecked")
    private final LazyOptional<IItemHandler>[] itemCaps = new LazyOptional[7];
    @SuppressWarnings("unchecked")
    private final LazyOptional<IFluidHandler>[] fluidCaps = new LazyOptional[7];

    protected AbstractEnergyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                        EnergyConfig config, SideConfig defaultSides,
                                        int slotCount, int... fluidTankCapacities) {
        super(type, pos, state);
        this.energyConfig = config;
        this.sideConfig = defaultSides;
        this.inventory = new ItemStackHandler(slotCount) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return AbstractEnergyBlockEntity.this.isItemValidForSlot(slot, stack);
            }
        };
        for (int cap : fluidTankCapacities) {
            if (cap > 0) {
                tanks.add(createFluidTank(cap));
            }
        }
    }

    /** Переопределяемая фабрика танка — для кастомных валидаторов. */
    protected FluidTank createFluidTank(int capacity) {
        return new FluidTank(capacity) {
            @Override
            protected void onContentsChanged() {
                setChanged();
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
            // Генератор начал/продолжил вырабатывать — сеть должна проснуться,
            // даже если до этого спала (чанк машины загрузился позже).
            PipeNetworkManager.wakeNear(level, worldPosition);
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

    /**
     * Внутренняя запись результата: setStackInSlot не проходит через isItemValid,
     * поэтому валидация ограничивает только игрока и соседей (трубы/хопперы),
     * но не сам механизм.
     */
    protected void insertResult(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) {
            inventory.setStackInSlot(slot, stack.copy());
            return;
        }
        if (ItemHandlerHelper.canItemStacksStack(current, stack)) {
            int room = current.getMaxStackSize() - current.getCount();
            int toAdd = Math.min(room, stack.getCount());
            if (toAdd > 0) {
                current.grow(toAdd);
                inventory.setStackInSlot(slot, current);
            }
        }
    }

    protected ItemStack stackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    // ═══════════════════════════════════════════════════════════════
    // Данные для GUI (меню читает только их)
    // ═══════════════════════════════════════════════════════════════

    /** Число слотов, которые меню покажет вверху окна. */
    public int getSlotCount() {
        return inventory.getSlots();
    }

    // ═══════════════════════════════════════════════════════════════
    // Жидкости
    // ═══════════════════════════════════════════════════════════════

    /** Число баков (0, если машина работает без жидкостей). */
    public int getTankCount() {
        return tanks.size();
    }

    /** Бак по индексу — для внутренней логики машины. */
    public FluidTank getTank(int index) {
        return tanks.get(index);
    }

    /** Сколько жидкости сейчас в N-м баке (для GUI). */
    public int getFluidAmount(int index) {
        return tanks.get(index).getFluidAmount();
    }

    /** Ёмкость N-го бака (для GUI). */
    public int getTankCapacity(int index) {
        return tanks.get(index).getCapacity();
    }

    /** Содержимое N-го бака (для GUI). */
    public FluidStack getFluidInTank(int index) {
        return tanks.get(index).getFluid();
    }

    /** Есть ли прогресс, который стоит рисовать (полоса/стрелка). */
    public boolean hasGuiProgress() {
        return false;
    }

    /** Прогресс текущего цикла 0..1. */
    public float getGuiProgress() {
        return 0f;
    }

    /** Занята ли машина делом прямо сейчас. */
    public boolean isWorking() {
        return false;
    }

    /** Заголовок окна = название блока. */
    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    /** Вызывается только на сервере при открытии окна. */
    @Override
    public @NotNull AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new MachineMenu(windowId, inv, this);
    }

    /** Окно живо, пока блок на месте и игрок рядом (ванильное правило 8 блоков). */
    public boolean stillValid(@NotNull Player player) {
        if (isRemoved()) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= MAX_USE_DISTANCE_SQR;
    }

    // ═══════════════════════════════════════════════════════════════
    // Ручная загрузка/выгрузка (до GUI)
    // ═══════════════════════════════════════════════════════════════

    /** Есть ли слот, в который игрок может что-то положить (для предсказания на клиенте). */
    public boolean hasInsertableSlot() {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * ПКМ с предметом в руке: вставить стак в первый пустой слот.
     */
    public boolean playerInsert(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack rest = inventory.insertItem(slot, held, false);
            if (rest.getCount() != held.getCount()) {
                player.setItemInHand(hand, rest);
                return true;
            }
        }
        return false;
    }

    /**
     * Shift+ПКМ пустой рукой: забрать стак из первого непустого слота.
     */
    public boolean playerExtract(Player player) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
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
            if (cap == ForgeCapabilities.FLUID_HANDLER && !tanks.isEmpty()) {
                return fluidCap(side).cast();
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
                cached = LazyOptional.of(() -> new SidedItemHandler(inventory, side, sideConfig));
            }
            itemCaps[index] = cached;
        }
        return cached;
    }

    private LazyOptional<IFluidHandler> fluidCap(@Nullable Direction side) {
        int index = side == null ? NULL_SIDE_INDEX : side.ordinal();
        LazyOptional<IFluidHandler> cached = fluidCaps[index];
        if (cached == null) {
            cached = LazyOptional.of(() -> new SidedFluidHandler(tanks, side, sideConfig));
            fluidCaps[index] = cached;
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
            if (fluidCaps[i] != null) {
                fluidCaps[i].invalidate();
                fluidCaps[i] = null;
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

    /**
     * Хук после изменения side-config: соседние трубы должны пересобрать
     * рукава — при DISABLED соединение рвётся и визуально, и в графе.
     */
    protected void onSideConfigChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            // neighborChanged у соседних труб: пересоберут рукава и пометят сеть грязной.
            level.updateNeighborsAt(worldPosition, state.getBlock());
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
        saveTanks(tag);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        energy = Math.max(0, Math.min(energyConfig.capacity(), tag.getInt("Energy")));
        sideConfig.load(tag);
        if (inventory.getSlots() > 0 && tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        loadTanks(tag);
    }

    /** Баки сериализуются в под-тег по индексу. */
    protected void saveTanks(CompoundTag tag) {
        if (tanks.isEmpty()) {
            return;
        }
        CompoundTag tanksTag = new CompoundTag();
        for (int i = 0; i < tanks.size(); i++) {
            tanksTag.put("Tank" + i, tanks.get(i).writeToNBT(new CompoundTag()));
        }
        tag.put("Tanks", tanksTag);
    }

    protected void loadTanks(CompoundTag tag) {
        if (tanks.isEmpty() || !tag.contains("Tanks")) {
            return;
        }
        CompoundTag tanksTag = tag.getCompound("Tanks");
        for (int i = 0; i < tanks.size(); i++) {
            String key = "Tank" + i;
            if (tanksTag.contains(key)) {
                tanks.get(i).readFromNBT(tanksTag.getCompound(key));
            }
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
        // Баки: синхронизируем содержимое (amount + fluid type).
        if (!tanks.isEmpty()) {
            CompoundTag tanksTag = new CompoundTag();
            for (int i = 0; i < tanks.size(); i++) {
                tanksTag.put("Tank" + i, tanks.get(i).getFluid().writeToNBT(new CompoundTag()));
            }
            tag.put("SyncTanks", tanksTag);
        }
    }

    protected void readSyncData(CompoundTag tag) {
        if (tag.contains("Energy")) {
            energy = Math.max(0, Math.min(energyConfig.capacity(), tag.getInt("Energy")));
        }
        sideConfig.load(tag);
        if (tag.contains("SyncTanks")) {
            CompoundTag tanksTag = tag.getCompound("SyncTanks");
            for (int i = 0; i < tanks.size(); i++) {
                String key = "Tank" + i;
                if (tanksTag.contains(key)) {
                    tanks.get(i).setFluid(FluidStack.loadFluidStackFromNBT(tanksTag.getCompound(key)));
                }
            }
        }
    }
}
