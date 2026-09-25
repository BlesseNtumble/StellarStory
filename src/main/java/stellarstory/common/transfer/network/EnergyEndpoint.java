package stellarstory.common.transfer.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Точка касания трубы и соседнего блока (машины).
 *
 * BlockCapabilityCache появился только в 1.20.2, поэтому кэш capability собран вручную:
 * держим последний BlockState цели и переспрашиваем capability только когда блок
 * поменялся либо истёк таймер периодической проверки.
 */
public final class EnergyEndpoint {

    /** Позиция машины (никогда не позиция трубы). */
    private final BlockPos machinePos;
    /** Грань машины, обращённая к трубе. */
    private final Direction machineFace;
    /** Грань трубы, обращённая к машине. */
    private final Direction pipeFace;

    @Nullable
    private BlockState lastState;
    @Nullable
    private LazyOptional<IEnergyStorage> cached;
    private int recheckTimer;

    public EnergyEndpoint(BlockPos machinePos, Direction machineFace, Direction pipeFace) {
        this.machinePos = machinePos.immutable();
        this.machineFace = machineFace;
        this.pipeFace = pipeFace;
    }

    public BlockPos machinePos() {
        return machinePos;
    }

    public Direction pipeFace() {
        return pipeFace;
    }

    /** @return capability энергии цели либо null, если цели нет / она недоступна. */
    @Nullable
    public IEnergyStorage storage(ServerLevel level, int recheckInterval) {
        BlockState state = level.isLoaded(machinePos) ? level.getBlockState(machinePos) : null;

        boolean valid = cached != null && state != null && state == lastState
                && ++recheckTimer < recheckInterval;
        if (valid) {
            return cached.resolve().orElse(null);
        }

        recheckTimer = 0;
        lastState = state;
        cached = null;

        // Блок без BlockEntity не может отдать capability — не дёргаем чанк зря.
        if (state == null || !state.hasBlockEntity()) {
            return null;
        }

        BlockEntity be = level.getBlockEntity(machinePos);
        if (be == null) {
            return null;
        }
        LazyOptional<IEnergyStorage> optional = be.getCapability(ForgeCapabilities.ENERGY, machineFace);
        // Сохраняем ленивую ссылку: если чужой BE будет удалён, он сам инвалидирует
        // свой LazyOptional, и resolve() вернёт пусто вместо обращения к мусору.
        cached = optional;
        return optional.resolve().orElse(null);
    }

    /** Сбросить кэш (используется при перестройке сети). */
    public void invalidate() {
        cached = null;
        lastState = null;
        recheckTimer = 0;
    }
}
