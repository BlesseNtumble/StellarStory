package stellarstory.common.transfer.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;
import stellarstory.api.transfer.ISideConfigurable;
import stellarstory.common.blocks.pipe.AbstractPipeBlock;
import stellarstory.common.config.StellarGeneralConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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

    /**
     * Период страховочного пробуждения спящих сетей, тиков. Сеть засыпает,
     * когда не переносит энергию, а разбудить её должны умеют не только
     * изменения графа, но и состояние машины (генератор начал гореть).
     */
    private static final int IDLE_WAKE_INTERVAL = 100;

    private final ServerLevel level;
    private final List<PipeNetwork> networks = new ArrayList<>();
    private final Map<BlockPos, PipeNetwork> networkByPos = new HashMap<>();
    private final Set<BlockPos> dirty = new HashSet<>();

    private final ArrayDeque<BlockPos> floodQueue = new ArrayDeque<>();
    private final Set<BlockPos> queued = new HashSet<>();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    /**
     * Чанки, загруженные позже (после WorldLoad), ждут сканирования в тике.
     * ChunkEvent.Load приходит с worker-потока, поэтому очередь конкурентная.
     */
    private final Queue<ChunkPos> chunkLoadQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

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

    /**
     * Менеджер создаём на загрузке мира, а не по первому событию трубы:
     * иначе события загрузки чанков придут раньше, и граф останется пустым.
     */
    public static void onLevelLoad(ServerLevel level) {
        MANAGERS.computeIfAbsent(level, l -> new PipeNetworkManager(level));
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

    /**
     * Граф в NBT не пишется, поэтому после перезахода мира он пуст.
     * Чанк догрузился — его трубы и машины должны встать в граф.
     *
     * ВАЖНО: это вызывается из ChunkEvent.Load, который может прийти с worker-
     * потока. Здесь только кладём позицию в очередь — сканирование идёт в tick.
     */
    public void onChunkLoaded(ChunkPos pos) {
        chunkLoadQueue.add(pos);
    }

    /**
     * Сканирование загруженных чанков: ищем трубы и машины, помечаем их грязными.
     * Вызывается из tickServer (главный поток).
     */
    private void processLoadedChunks() {
        int processed = 0;
        while (!chunkLoadQueue.isEmpty() && processed < 16) {
            ChunkPos pos = chunkLoadQueue.poll();
            if (level.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.FULL, false) instanceof LevelChunk chunk) {
                scanChunk(chunk);
            }
            processed++;
        }
    }

    /** Сканируем чанк: трубы внутри + машины → соседние трубы. */
    private void scanChunk(LevelChunk chunk) {
        int minY = level.getMinBuildHeight();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir()) {
                continue;
            }
            int yBase = minY + i * 16;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (section.getBlockState(x, y, z).getBlock() instanceof AbstractPipeBlock) {
                            dirty.add(new BlockPos(baseX + x, yBase + y, baseZ + z));
                        }
                    }
                }
            }
        }
        // Машины → помечаем соседние трубы грязными
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof ISideConfigurable) {
                seedAround(be.getBlockPos());
            }
        }
    }

    /** Трубы вокруг машины — кандидаты на перестройку сети. */
    public void seedAround(BlockPos machinePos) {
        for (Direction dir : DIRS) {
            cursor.setWithOffset(machinePos, dir);
            if (level.getBlockState(cursor).getBlock() instanceof AbstractPipeBlock) {
                dirty.add(cursor.immutable());
            }
        }
    }

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

        processLoadedChunks();
        rebuildDirty();

        int interval = StellarGeneralConfig.PIPE_TICK_INTERVAL.get();
        // Страховка от «засыпания»: сеть, которой в момент постройки не хватило
        // партнёра (чанк машины ещё не догрузился), спит и сама бы не проснулась.
        boolean wakeAll = level.getGameTime() % IDLE_WAKE_INTERVAL == 0;

        if (interval <= 1 || level.getGameTime() % interval == 0) {
            for (int i = 0; i < networks.size(); i++) {
                PipeNetwork network = networks.get(i);
                if (wakeAll) {
                    network.wake();
                }
                network.tick(level);
            }
        }

        lastTickNanos = System.nanoTime() - start;
        tickNanosTotal += lastTickNanos;
        tickSamples++;
    }

    /**
     * Разбудить сети труб рядом с позицией: машина начала производить энергию
     * (или изменила содержимое) — пассивная до этого сеть должна снова тикаться.
     *
     * Дёшево: только lookup по mapping, без перестройки графа.
     */
    public static void wakeNear(Level level, BlockPos pos) {
        PipeNetworkManager manager = MANAGERS.get(level);
        if (manager != null) {
            manager.wakeAround(pos);
        }
    }

    private void wakeAround(BlockPos pos) {
        for (Direction dir : DIRS) {
            cursor.setWithOffset(pos, dir);
            PipeNetwork network = networkByPos.get(cursor);
            if (network != null) {
                network.wake();
            }
        }
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
