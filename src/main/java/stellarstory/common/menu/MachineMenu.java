package stellarstory.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import stellarstory.common.blocks.blockentity.AbstractEnergyBlockEntity;
import stellarstory.common.init.StellarMenuTypes;
import net.minecraft.world.inventory.MenuType;

/**
 * Меню механизма: слоты BlockEntity + 36 слотов игрока.
 *
 * Состояние (энергия, прогресс, факт работы) летит на клиент через {@link ContainerData} —
 * это штатный механизм vanilla-синхронизации меню, отдельный пакет не нужен.
 * Сами стаки синхронизирует AbstractContainerMenu.
 */
public class MachineMenu extends AbstractContainerMenu {

    /**
     * Индексы полей в MachineData.
     *
     * ВАЖНО: vanilla отдаёт каждый слот ContainerData как short
     * (ClientboundContainerSetDataPacket пишет writeShort), то есть 16 бит со
     * знаком. Энергия 100 000 FE в него не влезает, поэтому большие числа
     * едут двумя половинками и собираются на клиенте в readInt.
     * Константы (ёмкость, вместимость бака) не синхронизируются вообще — они
     * одинаковы на обеих сторонах, потому что заданы в конструкторе блока.
     */
    public static final int DATA_ENERGY_LO = 0;
    public static final int DATA_ENERGY_HI = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_WORKING = 3;
    /** Далее по два поля на каждый бак: amount lo, amount hi. */
    public static final int DATA_TANK_BASE = 4;

    /** Ширина/высота окна и координаты сетки игрока — используют экраны. */
    public static final int WIDTH = 176;
    /**
     * 166 = низ хотбара (PLAYER_TOP+58+18), далее отступ + панель настройки
     * сторон (SideConfigPanel.HEIGHT) + нижний отступ.
     */
    public static final int HEIGHT = 216;
    public static final int PLAYER_TOP = 90;

    /** Расстояние, после которого vanilla закрывает окно. */
    private static final double MAX_DISTANCE_SQR = 64.0D;

    private final AbstractEnergyBlockEntity blockEntity;
    private final BlockPos blockPos;
    private final int machineSlotCount;
    private final MachineData data;

    /** Серверная сторона: BlockEntity уже в руках. */
    public MachineMenu(int windowId, Inventory playerInventory, AbstractEnergyBlockEntity blockEntity) {
        super(StellarMenuTypes.MACHINE_MENU.get(), windowId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.data = new MachineData(blockEntity);
        this.machineSlotCount = addMachineSlots();
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    /** Клиентская сторона: BlockEntity ищем в мире по позиции из буфера. */
    public MachineMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(windowId, playerInventory, resolve(playerInventory, buf.readBlockPos()));
    }

    private static AbstractEnergyBlockEntity resolve(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof AbstractEnergyBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No machine block entity at " + pos);
    }

    /**
     * Раскладка слотов машины: строка по центру сверху окна.
     * В GUI все слоты доступны руками — роли сторон (SideConfig) ограничивают
     * только соседей (трубы/хопперы), а не игрока.
     */
    private int addMachineSlots() {
        int total = blockEntity.getSlotCount();
        if (total <= 0) {
            return 0;
        }
        int startX = (WIDTH - (total * 18 + (total - 1) * 4)) / 2;
        for (int i = 0; i < total; i++) {
            addSlot(new SlotItemHandler(blockEntity.getInventory(), i, startX + i * 22, 20));
        }
        return total;
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, PLAYER_TOP + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, PLAYER_TOP + 58));
        }
    }

    /** Shift+клик: из машины в инвентарь и наоборот. */
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();

        boolean moved = index < machineSlotCount
                ? moveItemStackTo(source, machineSlotCount, slots.size(), true)
                : moveItemStackTo(source, 0, machineSlotCount, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (blockEntity.isRemoved() || blockEntity.getLevel() != player.level()) {
            return false;
        }
        return player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D)
                <= MAX_DISTANCE_SQR;
    }

    // ═══════════════════════════════════════════════════════════════
    // Данные для отрисовки (с клиента берутся из синхронизированного ContainerData)
    // ═══════════════════════════════════════════════════════════════

    public int getEnergy() {
        return readInt(DATA_ENERGY_LO);
    }

    /** Ёмкость известна на обеих сторонах (EnergyConfig блока) — не синхронизируется. */
    public int getMaxEnergy() {
        return blockEntity.getMaxEnergy();
    }

    public float getEnergyRatio() {
        int max = getMaxEnergy();
        return max <= 0 ? 0f : Math.min(1f, (float) getEnergy() / max);
    }

    /** Прогресс цикла в процентах 0..100. */
    public int getProgressPercent() {
        return data.get(DATA_PROGRESS);
    }

    public float getProgress() {
        return getProgressPercent() / 100f;
    }

    public boolean isWorking() {
        return data.get(DATA_WORKING) != 0;
    }

    public boolean hasProgress() {
        return data.hasProgress();
    }

    /** Количество баков. */
    public int getTankCount() {
        return data.getTanks();
    }

    public int getTankAmount(int index) {
        return readInt(DATA_TANK_BASE + index * 2);
    }

    /** Вместимость бака — конструкторская константа, читается прямо с BlockEntity. */
    public int getTankCapacity(int index) {
        return blockEntity.getTankCapacity(index);
    }

    /** Синхронизированный тип жидкости из NBT (строковый идентификатор). */
    public String getTankFluidId(int index) {
        return data.getFluidId(index);
    }

    public AbstractEnergyBlockEntity getBlockEntity() {
        return blockEntity;
    }

    /**
     * Сборка int из двух 16-битных слотов. readShort на клиенте знаковый,
     * поэтому обе половинки маскируются.
     */
    private int readInt(int lowSlot) {
        int lo = data.get(lowSlot) & 0xFFFF;
        int hi = data.get(lowSlot + 1) & 0xFFFF;
        return lo | (hi << 16);
    }

    private static int lo16(int value) {
        return value & 0xFFFF;
    }

    private static int hi16(int value) {
        return (value >>> 16) & 0xFFFF;
    }

    /**
     * Мост BlockEntity -> ContainerData.
     * Сервер: читает из BE напрямую.
     * Клиент: хранит кеш, который заполняет vanilla-механизм через set().
     */
    private static final class MachineData implements ContainerData {

        private static final int PROGRESS_STEPS = 100;

        private final AbstractEnergyBlockEntity be;
        private final int[] cache;
        private final boolean clientSide;

        private MachineData(AbstractEnergyBlockEntity be) {
            this.be = be;
            this.cache = new int[DATA_TANK_BASE + be.getTankCount() * 2];
            this.clientSide = be.getLevel() != null && be.getLevel().isClientSide();
        }

        @Override
        public int get(int index) {
            if (clientSide) {
                return cache[index];
            }
            return switch (index) {
                case DATA_ENERGY_LO -> lo16(be.getEnergy());
                case DATA_ENERGY_HI -> hi16(be.getEnergy());
                case DATA_PROGRESS -> (int) (be.getGuiProgress() * PROGRESS_STEPS);
                case DATA_WORKING -> be.isWorking() ? 1 : 0;
                default -> {
                    // Бак: чётный слот = младшая половина amount, нечётный = старшая
                    int tankIndex = (index - DATA_TANK_BASE) / 2;
                    if (tankIndex >= 0 && tankIndex < be.getTankCount()) {
                        int amount = be.getFluidAmount(tankIndex);
                        yield ((index - DATA_TANK_BASE) & 1) == 1 ? hi16(amount) : lo16(amount);
                    }
                    yield 0;
                }
            };
        }

        @Override
        public void set(int index, int value) {
            cache[index] = value;
        }

        @Override
        public int getCount() {
            return DATA_TANK_BASE + getTanks() * 2;
        }

        boolean hasProgress() {
            return be.hasGuiProgress();
        }

        /** Число баков — нужно для getCount(). */
        int getTanks() {
            return be.getTankCount();
        }

        /** Идентификатор жидкости в баке (для GUI). */
        String getFluidId(int index) {
            if (index >= 0 && index < be.getTankCount()) {
                var fluid = be.getFluidInTank(index);
                return fluid.isEmpty() ? "" : BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString();
            }
            return "";
        }
    }
}
