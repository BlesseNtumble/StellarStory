package stellarstory.common.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import stellarstory.api.energy.EnergyConfig;
import stellarstory.api.transfer.SideConfig;
import stellarstory.api.transfer.SlotRole;
import stellarstory.common.init.StellarBlockEntities;

/**
 * Аккумулятор: чистое хранилище энергии, инвентаря нет.
 * Переносом занимается сеть труб (модель A) — она вытягивает энергию через capability,
 * поэтому собственный тик нужен только для аккуратной синхронизации на клиент.
 */
public class EnergyBatteryBlockEntity extends AbstractEnergyBlockEntity {

    public static final int CAPACITY = 100_000;
    public static final int MAX_IO = 500;

    /** Максимальный период синхронизации энергии на клиент, тиков. */
    private static final int SYNC_INTERVAL = 20;

    /** Порог изменения, при котором шлём пакет сразу, не дожидаясь таймера. */
    private static final int SYNC_THRESHOLD = Math.max(1, CAPACITY / 32);

    private int syncTimer = 0;
    private int lastSyncedEnergy = -1;

    public EnergyBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(StellarBlockEntities.ENERGY_BATTERY.get(), pos, state,
                EnergyConfig.of(CAPACITY, MAX_IO, MAX_IO),
                SideConfig.batteryDefaults(),
                new SlotRole[0]);
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
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

    @Override
    protected void writeSyncData(@NotNull CompoundTag tag) {
        super.writeSyncData(tag);
        tag.putInt("MaxEnergy", energyConfig.capacity());
    }
}
