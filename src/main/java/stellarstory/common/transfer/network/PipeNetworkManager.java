package stellarstory.common.transfer.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import stellarstory.common.blocks.pipe.AbstractPipeBlock;
import stellarstory.common.config.StellarGeneralConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Граф труб измерения. Один экземпляр на ServerLevel.
 *
 * Ключевые решения по производительности:
 *   - события place/remove только помечают позиции грязными, граф перестраивается
 *     один раз за тик (коалесинг): линия из 20 сегментов = одна перестройка;
 *   - перестройка ограничена бюджетом сегментов за тик, чтобы worldgen/fill
 *     не подвесил сервер;
 *   - тик идёт от LevelTickEvent, а не от BlockEntityTicker сегмента.
 *
 * В NBT граф не пишется: он восстанавливается обходом чанков, в сохранение пойдёт
 * только содержимое сети, когда появятся накапливающие трубы.
 */
public final class PipeNetworkManager {

    private static final Map<Level, PipeNetworkManager> MANAGERS = new HashMap<>();
    private static final Direction[] DIRS = Direction.values();

    private final ServerLevel level;
    private final List<PipeNetwork> networks = new ArrayList<>();
    private final Map<BlockPos, PipeNetwork> networkByPos = new HashMap<>();
    private final Set<BlockPos> dirty = new HashSet<>();

    private final ArrayDeque<BlockPos> floodQueue = new ArrayDeque<>();
    private final Set<BlockPos> queued = new HashSet<>();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    // Статистика для /stellarstory pipe stats
    private long tickSamples = 0;
    private long tickNanosTotal = 0;
    private long lastTickNanos = 0;

    private PipeNetworkManager(ServerLevel level) {
        this.level = level;
    }

    // ═══════════════════════════════════════════════════════════════
    // Доступ и жизненный цикл
    // ═══════════════════════════════════════════════════════════════

    public static PipeNetworkManager get(Level level) {
        if (level.isClientSide) {
            throw new IllegalStateException("Нет графа труб на клиенте");
        }
        return MANAGERS.computeIfAbsent(level, l -> new PipeNetworkManager((ServerLevel) l));
    }

    @Nullable
    public static PipeNetworkManager find(Level level) {
        return MANAGERS.get(level);
    }

    public static void onLevelUnload(Level level) {
        MANAGERS.remove(level);
    }

    /** Вызывать из LevelTickEvent, Phase.END. */
    public static void tick(Level level) {
        PipeNetworkManager manager = MANAGERS.get(level);
        if (manager != null) {
            manager.tickServer();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // События графа
    // ═══════════════════════════════════════════════════════════════

    public void onPipePlaced(BlockPos pos) {
        dirty.add(pos.immutable());
    }

    public void onPipeRemoved(BlockPos pos) {
        dirty.add(pos.immutable());
    }

    /**
     * Сосед трубы изменился: рядом поставили или сломали машину.
     * Труба-сосед уже обработана onPlace/onRemove, поэтому её игнорируем —
     * это срезает основной шум от события.
     */
    public void onNeighborChanged(BlockPos pipePos, Block neighborBlock) {
        if (neighborBlock instanceof AbstractPipeBlock) {
            return;
        }
        dirty.add(pipePos.immutable());
    }

    // ═══════════════════════════════════════════════════════════════
    // Тик
    // ═══════════════════════════════════════════════════════════════

    private void tickServer() {
        long start = System.nanoTime();

        rebuildDirty();

        int interval = StellarGeneralConfig.PIPE_TICK_INTERVAL.get();
        if (interval <= 1 || level.getGameTime() % interval == 0) {
            for (int i = 0; i < networks.size(); i++) {
                networks.get(i).tick(level);
            }
        }

        lastTickNanos = System.nanoTime() - start;
        tickNanosTotal += lastTickNanos;
        tickSamples++;
    }

    // ═══════════════════════════════════════════════════════════════
    // Перестройка графа
    // ═══════════════════════════════════════════════════════════════

    private void rebuildDirty() {
        if (dirty.isEmpty()) {
            return;
        }

        int budget = StellarGeneralConfig.PIPE_REBUILD_BUDGET.get();
        List<BlockPos> seeds = new ArrayList<>();
        Set<PipeNetwork> affected = new HashSet<>();

        for (Iterator<BlockPos> it = dirty.iterator(); it.hasNext(); ) {
            BlockPos pos = it.next();
            it.remove();
            seeds.add(pos);
            PipeNetwork network = networkByPos.remove(pos);
            if (network != null && affected.add(network)) {
                seeds.addAll(network.segments());
            }
        }

        // Старые сети затронутых позиций выбрасываются целиком: слияние и разрыв
        // линий после этого получается самим обходом, без отдельной логики.
        for (PipeNetwork network : affected) {
            for (BlockPos segment : network.segments()) {
                networkByPos.remove(segment);
            }
            networks.remove(network);
        }

        for (int i = 0; i < seeds.size(); i++) {
            BlockPos seed = seeds.get(i);
            if (networkByPos.containsKey(seed)) {
                continue;
            }
            Block block = level.getBlockState(seed).getBlock();
            if (!(block instanceof AbstractPipeBlock)) {
                continue;
            }
            if (budget <= 0) {
                // Бюджет тика исчерпан — дозаполним в следующем тике.
                dirty.add(seed);
                continue;
            }
            EnergyNetwork network = new EnergyNetwork(block);
            int used = flood(seed, network, budget);
            if (used < 0) {
                // Недобрали бюджет — сеть не собрана, попробуем в следующем тике.
                dirty.add(seed);
                continue;
            }
            budget -= used;
            if (network.segments().isEmpty()) {
                continue;
            }
            networks.add(network);
            network.rebuildEndpoints(level);
        }
    }

    /** Обход в ширину от сегмента по соседним трубам того же типа. */
    private int flood(BlockPos seed, PipeNetwork network, int budget) {
        Block type = network.pipeBlock();
        floodQueue.clear();
        queued.clear();
        floodQueue.add(seed);
        queued.add(seed);

        int visited = 0;

        while (!floodQueue.isEmpty()) {
            BlockPos current = floodQueue.poll();
            // В mapping позиция попадает только когда реально обработана:
            // иначе при недоборе бюджета останутся «закрытые», но не развёрнутые сегменты.
            network.segments().add(current);
            networkByPos.put(current, network);
            visited++;

            for (Direction dir : DIRS) {
                cursor.setWithOffset(current, dir);
                if (queued.contains(cursor) || level.getBlockState(cursor).getBlock() != type) {
                    continue;
                }
                BlockPos neighbor = cursor.immutable();
                queued.add(neighbor);
                floodQueue.add(neighbor);
            }

            if (visited >= budget) {
                // Бюджет исчерпан: откатываем частичный результат, перестроим целиком в следующем тике.
                for (BlockPos segment : network.segments()) {
                    networkByPos.remove(segment);
                }
                network.segments().clear();
                return -1;
            }
        }
        return visited;
    }

    // ═══════════════════════════════════════════════════════════════
    // Статистика
    // ═══════════════════════════════════════════════════════════════

    public int networkCount() {
        return networks.size();
    }

    public int activeNetworkCount() {
        int active = 0;
        for (int i = 0; i < networks.size(); i++) {
            if (!networks.get(i).isIdle()) {
                active++;
            }
        }
        return active;
    }

    public int segmentCount() {
        return networkByPos.size();
    }

    public int endpointCount() {
        int total = 0;
        for (int i = 0; i < networks.size(); i++) {
            total += networks.get(i).endpointCount();
        }
        return total;
    }

    public double averageTickMicros() {
        return tickSamples == 0 ? 0.0 : (tickNanosTotal / (double) tickSamples) / 1000.0;
    }

    public double lastTickMicros() {
        return lastTickNanos / 1000.0;
    }

    public int dirtyCount() {
        return dirty.size();
    }
}
