package stellarstory.common.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import stellarstory.api.energy.EnergyConfig;
import stellarstory.api.transfer.SideConfig;
import stellarstory.common.blocks.AbstractEnergyBlock;

/**
 * Машина-потребитель: тратит энергию за тик и крутит цикл прогресса.
 * Без энергии прогресс замирает, но не сгорает.
 */
public abstract class AbstractMachineBlockEntity extends AbstractEnergyBlockEntity {

    protected int progress = 0;
    protected boolean running = false;

    protected AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                         EnergyConfig config, SideConfig defaultSides,
                                         int slotCount, int... fluidTankCapacities) {
        super(type, pos, state, config, defaultSides, slotCount, fluidTankCapacities);
    }

    /** FE, сгорающие за один тик работы. */
    protected abstract int getEnergyPerTick();

    /** Тиков на полный цикл. */
    protected abstract int getOperateTime();

    /** Есть ли вход и свободное место в выходе. */
    protected abstract boolean canProcess();

    /** Результат завершённого цикла. */
    protected abstract void finishProgress();

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        boolean canWork = canProcess() && energy >= getEnergyPerTick();
        if (canWork) {
            consumeEnergy(getEnergyPerTick());
            running = true;
            if (++progress >= getOperateTime()) {
                finishProgress();
                progress = 0;
            }
        } else {
            running = false;
        }
        syncRunningState();
    }

    /** Прокидываем свойство RUNNING на клиент (подсветка работающего блока). */
    protected void syncRunningState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState current = getBlockState();
        if (!current.hasProperty(AbstractEnergyBlock.RUNNING)) {
            return;
        }
        if (current.getValue(AbstractEnergyBlock.RUNNING) != running) {
            level.setBlock(worldPosition, current.setValue(AbstractEnergyBlock.RUNNING, running), Block.UPDATE_CLIENTS);
        }
    }

    public boolean isRunning() {
        return running;
    }

    /** Прогресс цикла 0..1 для будущего GUI. */
    public float getProgress() {
        int total = getOperateTime();
        return total <= 0 ? 0f : (float) progress / total;
    }

    /** Энергия 0..1 для будущего GUI. */
    public float getEnergyRatio() {
        return energyConfig.capacity() <= 0 ? 0f : (float) energy / energyConfig.capacity();
    }

    @Override
    public boolean hasGuiProgress() {
        return true;
    }

    @Override
    public float getGuiProgress() {
        return getProgress();
    }

    @Override
    public boolean isWorking() {
        return running;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Progress", progress);
        tag.putBoolean("Running", running);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        progress = Math.max(0, tag.getInt("Progress"));
        running = tag.getBoolean("Running");
    }
}
