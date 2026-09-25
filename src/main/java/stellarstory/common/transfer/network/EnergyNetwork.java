package stellarstory.common.transfer.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.energy.IEnergyStorage;
import stellarstory.common.config.StellarGeneralConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Энергосеть по модели A («шина»): труба ничего не хранит, у сети нет ёмкости,
 * энергия внутри сети не маршрутизируется.
 *
 * Тик состоит из трёх фаз:
 *   1. simulate-опрос источников — сколько FE готово поступить;
 *   2. реальный забор этого объёма у источников;
 *   3. реальная раздача потребителям (сначала чистые потребители, затем накопители),
 *      а не разобранное возвращается источникам.
 *
 * Стоимость тика — O(эндпоинтов): длина линии из 20 и из 2000 сегментов стоит одинаково.
 */
public final class EnergyNetwork extends PipeNetwork {

    private static final Direction[] DIRS = Direction.values();

    private final List<EnergyEndpoint> endpoints = new ArrayList<>();

    // Переиспользуемые буферы: в тике не аллоцируем списки и не боксим int.
    private final List<IEnergyStorage> sources = new ArrayList<>();
    private final List<IEnergyStorage> consumers = new ArrayList<>();
    private final List<IEnergyStorage> buffers = new ArrayList<>();
    private int[] sourceWants = new int[0];

    /**
     * Остаток, который не удалось раздать в прошлом тике (источник отдал больше,
     * чем приняли потребители). Хранится, чтобы энергия не исчезала, и первым делом
     * раздаётся на следующем тике. В норме всегда 0.
     */
    private int residual = 0;

    public EnergyNetwork(Block pipeBlock) {
        super(pipeBlock);
    }

    @Override
    public int endpointCount() {
        return endpoints.size();
    }

    @Override
    public void rebuildEndpoints(ServerLevel level) {
        endpoints.clear();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (BlockPos segment : segments) {
            for (Direction dir : DIRS) {
                cursor.setWithOffset(segment, dir);
                // Сосед-труба этой же сети — ребро, а не эндпоинт.
                if (level.getBlockState(cursor).getBlock() == pipeBlock) {
                    continue;
                }
                // pipeFace — грань трубы, machineFace — противоположная ей грань машины.
                endpoints.add(new EnergyEndpoint(cursor.immutable(), dir.getOpposite(), dir));
            }
        }
        wake();
    }

    @Override
    protected boolean doTick(ServerLevel level) {
        if (endpoints.isEmpty()) {
            return false;
        }
        int limit = StellarGeneralConfig.PIPE_MAX_FE_PER_TICK.get();
        int recheck = StellarGeneralConfig.PIPE_ENDPOINT_RECHECK.get();

        sources.clear();
        consumers.clear();
        buffers.clear();

        int available = residual;
        int sourceCount = 0;

        // Фаза 1: сколько источники готовы отдать (только simulate, без мутаций).
        for (int i = 0; i < endpoints.size(); i++) {
            IEnergyStorage storage = endpoints.get(i).storage(level, recheck);
            if (storage == null) {
                continue;
            }
            int give = storage.extractEnergy(limit, true);
            int room = storage.receiveEnergy(limit, true);

            if (give > 0) {
                sourceWants = ensureCapacity(sourceWants, sourceCount + 1);
                sourceWants[sourceCount++] = give;
                sources.add(storage);
                available += give;
            }
            if (room > 0) {
                // Тот, кто умеет и отдавать, — накопитель: сначала кормим чистых потребителей.
                if (give > 0) {
                    buffers.add(storage);
                } else {
                    consumers.add(storage);
                }
            }
        }

        if (available <= 0 || (consumers.isEmpty() && buffers.isEmpty())) {
            return false;
        }

        // Фаза 2: реально забираем у источников не больше пропускной способности сети.
        // residual (недорозданный остаток прошлого тика) тоже входит в бюджет
        // и раздаётся вместе со свежевзятым — иначе энергия исчезает.
        int budget = Math.min(available, limit);
        int taken = 0;
        for (int i = 0; i < sources.size() && taken + residual < budget; i++) {
            int want = Math.min(budget - taken - residual, sourceWants[i]);
            taken += sources.get(i).extractEnergy(want, false);
        }

        // Фаза 3: раздаём взятое + остаток; непринятое возвращаем источникам, а не уничтожаем.
        int toDistribute = taken + residual;
        if (toDistribute <= 0) {
            return false;
        }
        int delivered = distribute(toDistribute);
        int leftover = toDistribute - delivered;
        if (leftover > 0) {
            for (int i = 0; i < sources.size() && leftover > 0; i++) {
                leftover -= sources.get(i).receiveEnergy(leftover, false);
            }
        }
        residual = leftover;
        return delivered > 0;
    }

    /** Раздача по потребителям: сначала чистые, затем накопители. @return сколько реально принято */
    private int distribute(int amount) {
        int left = amount;
        left -= pushTo(consumers, left);
        if (left > 0) {
            left -= pushTo(buffers, left);
        }
        return amount - left;
    }

    private int pushTo(List<IEnergyStorage> targets, int amount) {
        int left = amount;
        for (int i = 0; i < targets.size() && left > 0; i++) {
            left -= targets.get(i).receiveEnergy(left, false);
        }
        return amount - left;
    }

    private static int[] ensureCapacity(int[] array, int size) {
        if (array.length >= size) {
            return array;
        }
        int[] grown = new int[Math.max(size, array.length * 2 + 4)];
        System.arraycopy(array, 0, grown, 0, array.length);
        return grown;
    }
}
