package stellarstory.common.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import stellarstory.api.energy.EnergyConfig;
import stellarstory.api.transfer.SideConfig;
import stellarstory.common.init.StellarBlockEntities;

/**
 * Аккумулятор: внутреннее хранилище энергии + 2 слота энергопредметов с ЖЁСТКИМИ
 * ролями: SLOT_CHARGE (0) — блок только заряжает предмет, SLOT_DISCHARGE (1) —
 * предмет только разряжается в блок.
 *
 * Обмен идёт через стандартный FE-capability стака (любой IEnergyStorage),
 * поэтому работает с любыми FE-совместимыми предметами.
 *
 * На клиент энергия синхронизируется через getUpdateTag() (см. AbstractEnergyBlockEntity).
 */
public class EnergyBatteryBlockEntity extends AbstractEnergyBlockEntity {

    public static final int CAPACITY = 100_000;
    public static final int MAX_IO = 500;

    /** Слот выдачи энергии предмету. */
    public static final int SLOT_CHARGE = 0;
    /** Слот приёма энергии от предмета. */
    public static final int SLOT_DISCHARGE = 1;

    private static final int SLOT_COUNT = 2;

    /** Максимальный период синхронизации энергии на клиент, тиков. */
    private static final int SYNC_INTERVAL = 20;

    /** Порог изменения, при котором шлём пакет сразу, не дожидаясь таймера. */
    private static final int SYNC_THRESHOLD = Math.max(1, CAPACITY / 32);

    private int syncTimer = 0;
    private int lastSyncedEnergy = -1;

    public EnergyBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(StellarBlockEntities.ENERGY_BATTERY.get(), pos, state,
                EnergyConfig.bidirectional(CAPACITY, MAX_IO),
                SideConfig.batteryDefaults(),
                SLOT_COUNT);
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }

        // 1. Перенос энергии: разрядка предмета -> блок, затем блок -> предмет.
        //    Обратный порядок важен: принятая из SLOT_DISCHARGE энергия в этом же
        //    тике может уйти в SLOT_CHARGE, иначе при перестановке ячеек заряд
        //    шёл бы через тик задержки.
        chargeFromItem();
        dischargeToItem();

        // 2. Периодическая синхронизация на клиент
        boolean timeElapsed = ++syncTimer >= SYNC_INTERVAL;
        boolean changedEnough = Math.abs(energy - lastSyncedEnergy) >= SYNC_THRESHOLD;
        if (!timeElapsed && !changedEnough) {
            return;
        }
        syncTimer = 0;
        lastSyncedEnergy = energy;
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    /** Слот 0: блок отдаёт энергию предмету, сколько тот возьмёт. */
    private void dischargeToItem() {
        IEnergyStorage itemEnergy = itemEnergy(SLOT_CHARGE);
        if (itemEnergy == null) {
            return;
        }
        int want = itemEnergy.receiveEnergy(MAX_IO, true);
        if (want > 0 && consumeEnergy(want)) {
            itemEnergy.receiveEnergy(want, false);
        }
    }

    /** Слот 1: предмет отдаёт энергию блоку, сколько тот примет. */
    private void chargeFromItem() {
        IEnergyStorage itemEnergy = itemEnergy(SLOT_DISCHARGE);
        if (itemEnergy == null) {
            return;
        }
        int room = energyConfig.capacity() - energy;
        int available = itemEnergy.extractEnergy(Math.min(MAX_IO, room), true);
        if (available > 0) {
            itemEnergy.extractEnergy(available, false);
            produceEnergy(available);
        }
    }

    @Nullable
    private IEnergyStorage itemEnergy(int slot) {
        ItemStack stack = stackInSlot(slot);
        if (stack.isEmpty()) {
            return null;
        }
        return stack.getCapability(ForgeCapabilities.ENERGY, null).orElse(null);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, @NotNull ItemStack stack) {
        // Разрешаем предметы, предоставляющие FE capability
        return stack.getCapability(ForgeCapabilities.ENERGY, null).isPresent();
    }

    @Override
    protected void writeSyncData(@NotNull CompoundTag tag) {
        super.writeSyncData(tag);
        tag.putInt("MaxEnergy", energyConfig.capacity());
    }
}
