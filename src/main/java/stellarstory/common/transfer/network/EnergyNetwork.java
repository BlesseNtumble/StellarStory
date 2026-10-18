package stellarstory.common.transfer.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.energy.IEnergyStorage;
import stellarstory.api.transfer.ISideConfigurable;
import stellarstory.api.transfer.SideMode;
import stellarstory.api.transfer.TransferKind;
import stellarstory.common.config.StellarGeneralConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Энергосеть по модели A («шина»): труба ничего не хранит, у сети нет ёмкости,
 * энергия внутри сети не маршрутизируется.
 *
 * Тик состоит из трёх фаз:
 *   1. simulate-опрос эндпоинтов: кто сколько готов отдать (pureSupply — чистые
 *      источники, bufferSupply — накопители) и кто сколько готов принять (demand);
 *   2. реальный забор: budget = min(supply, demand, пропускная способность).
 *      Накопитель выступает источником только когда чистых источников нет —
 *      иначе он вычитает FE сам у себя и съедает бюджет;
 *   3. раздача: сначала чистые потребители, затем накопители; не принято
 *      возвращается чистым источникам, а они не приняли — остаётся в residual
 *      и раздаётся следующим тиком.
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
                BlockState neighbor = level.getBlockState(cursor);
                // Сосед-труба этой же сети — ребро, а не эндпоинт.
                if (neighbor.getBlock() == pipeBlock || !neighbor.hasBlockEntity()) {
                    continue;
                }
                Direction machineFace = dir.getOpposite();
                BlockEntity be = level.getBlockEntity(cursor);
                if (be instanceof ISideConfigurable configurable) {
                    SideMode mode = configurable.getSideConfig().get(TransferKind.ENERGY, machineFace);
                    if (!mode.canReceive() && !mode.canExtract()) {
                        continue; // сторона отключена — контакта нет
                    }
                }
                // pipeFace — грань трубы, machineFace — противоположная ей грань машины.
                endpoints.add(new EnergyEndpoint(cursor.immutable(), machineFace, dir));
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

        int sourceCount = 0;
        int pureSupply = 0;
        int bufferSupply = 0;
        int demand = 0;

        // Фаза 1: сколько источники готовы отдать и потребители принять (simulate, без мутаций).
        //
        // Разделение принципиальное: накопитель (может и отдавать, и принимать) — это
        // источник только в крайнем случае. Иначе он забирает FE сам у себя, съедает
        // бюджет сети, и генератор в этот тик не выгружается вовсе.
        for (int i = 0; i < endpoints.size(); i++) {
            IEnergyStorage storage = endpoints.get(i).storage(level, recheck);
            if (storage == null) {
                continue;
            }
            int give = storage.extractEnergy(limit, true);
            int room = storage.receiveEnergy(limit, true);

            if (room > 0) {
                // Бюджет всё равно упирается в limit, поэтому накопление режем по нему:
                // так исключаем переполнение int на большой сети.
                demand = Math.min(limit, demand + room);
                if (give > 0) {
                    buffers.add(storage);
                } else {
                    consumers.add(storage);
                }
            }
            if (give > 0) {
                if (room > 0) {
                    bufferSupply = Math.min(limit, bufferSupply + give);
                } else {
                    sourceWants = ensureCapacity(sourceWants, sourceCount + 1);
                    sourceWants[sourceCount++] = give;
                    sources.add(storage);
                    pureSupply = Math.min(limit, pureSupply + give);
                }
            }
        }

        int supply = pureSupply > 0 ? pureSupply : bufferSupply;
        if (supply <= 0 || demand <= 0) {
            return false;
        }

        // Фаза 2: забираем не больше пропускной способности И не больше реального спроса.
        // Ограничение по спросу важнее, чем кажется: сверх принятого отдать некуда,
        // а чистый источник (maxReceive = 0) непринятое не примет — оно осело бы в
        // residual и следующими тиками блокировало бы новый забор.
        //
        // Остаток прошлого тика в бюджет не входит: он раздается сверху, иначе
        // накопленный residual обнулил бы want и сеть встала бы с полным генератором.
        int budget = Math.min(Math.min(supply, demand), limit);
        int taken = 0;

        if (budget > 0) {
            if (pureSupply > 0) {
                for (int i = 0; i < sources.size() && taken < budget; i++) {
                    taken += sources.get(i).extractEnergy(Math.min(budget - taken, sourceWants[i]), false);
                }
            } else if (!consumers.isEmpty()) {
                // Чистых источников нет — разряжаем накопители в чистых потребителей.
                // Без потребителей не начинаем: иначе батарея отдаёт FE сама себе,
                // distribute возвращает их же обратно, и сеть крутит холостой цикл.
                for (int i = 0; i < buffers.size() && taken < budget; i++) {
                    taken += buffers.get(i).extractEnergy(budget - taken, false);
                }
            }
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
