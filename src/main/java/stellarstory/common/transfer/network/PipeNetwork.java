package stellarstory.common.transfer.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import stellarstory.common.config.StellarGeneralConfig;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Слитая сеть труб: все связанные сегменты одного типа — один объект с одним тиком.
 * Сегменты сами ничего не хранят и не тикаются, поэтому стоимость тика зависит от числа
 * эндпоинтов (машин), а не от длины линии.
 */
public abstract class PipeNetwork {

    /** Тип трубы: сегменты разных типов в одну сеть не сливаются. */
    protected final Block pipeBlock;

    /** Позиции сегментов. Только immutable-ключи. */
    protected final Set<BlockPos> segments = new LinkedHashSet<>();

    protected boolean idle = false;
    protected int idleTicks = 0;
    protected PipeNetwork(Block pipeBlock) {
        this.pipeBlock = pipeBlock;
    }

    public Block pipeBlock() {
        return pipeBlock;
    }

    public Set<BlockPos> segments() {
        return segments;
    }

    public boolean isIdle() {
        return idle;
    }

    public int endpointCount() {
        return 0;
    }

    /** Разбудить сеть (изменился граф или состояние эндпоинтов). */
    public void wake() {
        idle = false;
        idleTicks = 0;
    }

    /**
     * Один тик сети. Периодичность задаёт менеджер, здесь только учёт простоя:
     * сеть, которая не переносит энергию, засыпает и не тикается до изменения графа.
     *
     * @return true, если что-то перенесено
     */
    public boolean tick(ServerLevel level) {
        if (idle) {
            return false;
        }
        boolean moved = doTick(level);
        if (moved) {
            idleTicks = 0;
        } else if (++idleTicks >= StellarGeneralConfig.PIPE_IDLE_AFTER.get()) {
            idle = true;
        }
        return moved;
    }

    protected abstract boolean doTick(ServerLevel level);

    /** Пересобрать список эндпоинтов после изменения графа. */
    public abstract void rebuildEndpoints(ServerLevel level);
}
