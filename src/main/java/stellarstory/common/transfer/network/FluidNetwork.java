package stellarstory.common.transfer.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import stellarstory.api.transfer.ISideConfigurable;
import stellarstory.api.transfer.SideMode;
import stellarstory.api.transfer.TransferKind;
import stellarstory.common.blocks.pipe.AbstractPipeBlock;
import stellarstory.common.blocks.pipe.FluidPipeBlockEntity;
import stellarstory.common.config.StellarGeneralConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Жидкостная сеть: труба ничего не хранит, но рендерит жидкость, которая сейчас
 * переносится между эндпоинтами.
 *
 * В отличие от энергосети, здесь нет накопителей: переносим первую найденную
 * жидкость из эндпоинтов-источников в эндпоинты-приёмники до упора в пропускную
 * способность.
 */
public final class FluidNetwork extends PipeNetwork {

    private static final Direction[] DIRS = Direction.values();
    private static final int VISUAL_AMOUNT = 1000;

    private final List<FluidEndpoint> endpoints = new ArrayList<>();
    private final List<IFluidHandler> sources = new ArrayList<>();
    private final List<IFluidHandler> consumers = new ArrayList<>();
    private final List<BlockPos> sourcePositions = new ArrayList<>();
    private final List<BlockPos> consumerPositions = new ArrayList<>();

    private FluidStack visualFluid = FluidStack.EMPTY;
    private boolean visualDirty = false;

    public FluidNetwork(Block pipeBlock) {
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
                // Соседняя труба — ребро графа, а не эндпоинт переноса.
                if (neighbor.getBlock() instanceof AbstractPipeBlock || !neighbor.hasBlockEntity()) {
                    continue;
                }
                Direction machineFace = dir.getOpposite();
                BlockEntity be = level.getBlockEntity(cursor);
                if (be instanceof ISideConfigurable configurable) {
                    SideMode mode = configurable.getSideConfig().get(TransferKind.FLUID, machineFace);
                    if (!mode.canReceive() && !mode.canExtract()) {
                        continue; // сторона отключена — контакта нет
                    }
                }
                // pipeFace — грань трубы, machineFace — противоположная ей грань машины.
                endpoints.add(new FluidEndpoint(cursor.immutable(), machineFace, dir));
            }
        }
        wake();
    }

    @Override
    protected boolean doTick(ServerLevel level) {
        if (endpoints.isEmpty()) {
            setVisual(FluidStack.EMPTY);
            applyVisual(level);
            return false;
        }

        int limit = StellarGeneralConfig.PIPE_MAX_MB_PER_TICK.get();
        int recheck = StellarGeneralConfig.PIPE_ENDPOINT_RECHECK.get();

        sources.clear();
        consumers.clear();
        sourcePositions.clear();
        consumerPositions.clear();

        // Фаза 1: выбираем тип жидкости, который будет переноситься в этом тике.
        FluidStack transferType = FluidStack.EMPTY;
        for (int i = 0; i < endpoints.size(); i++) {
            IFluidHandler handler = endpoints.get(i).handler(level, recheck);
            if (handler == null) {
                continue;
            }
            FluidStack extracted = handler.drain(limit, IFluidHandler.FluidAction.SIMULATE);
            if (!extracted.isEmpty()) {
                transferType = extracted.copy();
                break;
            }
        }

        if (transferType.isEmpty()) {
            setVisual(FluidStack.EMPTY);
            applyVisual(level);
            return false;
        }

        // Фаза 2: собираем источники и потребители именно этого типа.
        for (int i = 0; i < endpoints.size(); i++) {
            FluidEndpoint endpoint = endpoints.get(i);
            IFluidHandler handler = endpoint.handler(level, recheck);
            if (handler == null) {
                continue;
            }
            FluidStack simulateDrain = handler.drain(transferType.copy(), IFluidHandler.FluidAction.SIMULATE);
            FluidStack request = transferType.copy();
            request.setAmount(limit);
            int room = handler.fill(request, IFluidHandler.FluidAction.SIMULATE);

            if (room > 0) {
                consumers.add(handler);
                consumerPositions.add(endpoint.machinePos());
            }
            if (!simulateDrain.isEmpty()) {
                sources.add(handler);
                sourcePositions.add(endpoint.machinePos());
            }
        }

        if (sources.isEmpty() || consumers.isEmpty()) {
            setVisual(transferType);
            applyVisual(level);
            return false;
        }

        int moved = transfer(limit, transferType);
        setVisual(transferType);
        applyVisual(level);
        return moved > 0;
    }

    private int transfer(int limit, FluidStack transferType) {
        int moved = 0;
        for (int c = 0; c < consumers.size() && moved < limit; c++) {
            IFluidHandler consumer = consumers.get(c);
            BlockPos consumerPos = consumerPositions.get(c);

            FluidStack request = transferType.copy();
            request.setAmount(limit - moved);
            int want = consumer.fill(request, IFluidHandler.FluidAction.SIMULATE);
            if (want <= 0) {
                continue;
            }

            int filledHere = 0;
            for (int s = 0; s < sources.size() && filledHere < want; s++) {
                if (sourcePositions.get(s).equals(consumerPos)) {
                    continue;
                }

                IFluidHandler source = sources.get(s);
                FluidStack extractRequest = transferType.copy();
                extractRequest.setAmount(want - filledHere);
                FluidStack extracted = source.drain(extractRequest, IFluidHandler.FluidAction.SIMULATE);
                if (extracted.isEmpty()) {
                    continue;
                }

                FluidStack executed = source.drain(extracted, IFluidHandler.FluidAction.EXECUTE);
                if (executed.isEmpty()) {
                    continue;
                }

                int amount = executed.getAmount();
                FluidStack toFill = transferType.copy();
                toFill.setAmount(amount);
                int filled = consumer.fill(toFill, IFluidHandler.FluidAction.EXECUTE);

                if (filled < amount) {
                    FluidStack refund = executed.copy();
                    refund.setAmount(amount - filled);
                    source.fill(refund, IFluidHandler.FluidAction.EXECUTE);
                }

                moved += filled;
                filledHere += filled;
            }
        }
        return moved;
    }

    private void setVisual(FluidStack stack) {
        FluidStack next = stack.isEmpty() ? FluidStack.EMPTY : new FluidStack(stack.getFluid(), VISUAL_AMOUNT, stack.getTag());
        if (isSameFluid(visualFluid, next)) {
            return;
        }
        visualFluid = next;
        visualDirty = true;
    }

    private void applyVisual(ServerLevel level) {
        if (!visualDirty) {
            return;
        }
        visualDirty = false;
        for (BlockPos segment : segments) {
            BlockEntity be = level.getBlockEntity(segment);
            if (be instanceof FluidPipeBlockEntity pipeEntity) {
                pipeEntity.setRenderedFluid(visualFluid);
            }
        }
    }

    private static boolean isSameFluid(@Nullable FluidStack left, @Nullable FluidStack right) {
        if (left == null || left.isEmpty()) {
            return right == null || right.isEmpty();
        }
        return right != null && !right.isEmpty() && left.isFluidEqual(right);
    }

    // ═══════════════════════════════════════════════════════════════
    // Proxy API для BlockEntity трубы
    //
    // Те же риски реентерабельности, что и в EnergyNetwork — используем
    // отдельные списки, чтобы не трогать sources/consumers основного тика.
    // ═══════════════════════════════════════════════════════════════

    private final List<IFluidHandler> proxyTargets = new ArrayList<>();

    /**
     * Заполнение через трубу: передаём жидкость потребителям сети.
     * В самой трубе жидкость не задерживается.
     */
    public int proxyFill(ServerLevel level, @Nullable Direction face,
                         FluidStack resource, IFluidHandler.FluidAction action) {
        if (segments.isEmpty() || resource.isEmpty()) {
            return 0;
        }
        int limit = StellarGeneralConfig.PIPE_MAX_MB_PER_TICK.get();
        int want = Math.min(resource.getAmount(), limit);

        proxyTargets.clear();
        int room = 0;
        for (int i = 0; i < endpoints.size(); i++) {
            if (endpoints.get(i).pipeFace() == face) {
                continue;
            }
            IFluidHandler handler = endpoints.get(i).handler(level, 1);
            if (handler == null) {
                continue;
            }
            FluidStack copy = resource.copy();
            copy.setAmount(want);
            int here = handler.fill(copy, IFluidHandler.FluidAction.SIMULATE);
            if (here > 0) {
                proxyTargets.add(handler);
                room = Math.min(want, room + here);
            }
        }
        if (room <= 0) {
            return 0;
        }

        int filled = 0;
        for (int i = 0; i < proxyTargets.size() && filled < want; i++) {
            FluidStack part = resource.copy();
            part.setAmount(want - filled);
            filled += proxyTargets.get(i).fill(part, action);
        }
        if (filled > 0) {
            setVisual(resource);
            applyVisual(level);
            wake();
        }
        return filled;
    }

    /**
     * Забор жидкости через трубу: берём от источников сети.
     */
    public FluidStack proxyDrain(ServerLevel level, @Nullable Direction face,
                                  int maxDrain, IFluidHandler.FluidAction action) {
        if (segments.isEmpty() || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        int limit = StellarGeneralConfig.PIPE_MAX_MB_PER_TICK.get();
        int want = Math.min(maxDrain, limit);

        proxyTargets.clear();
        int drainable = 0;
        for (int i = 0; i < endpoints.size(); i++) {
            if (endpoints.get(i).pipeFace() == face) {
                continue;
            }
            IFluidHandler handler = endpoints.get(i).handler(level, 1);
            if (handler == null) {
                continue;
            }
            FluidStack canDrain = handler.drain(want, IFluidHandler.FluidAction.SIMULATE);
            if (!canDrain.isEmpty()) {
                proxyTargets.add(handler);
                drainable = Math.min(want, drainable + canDrain.getAmount());
            }
        }
        if (drainable <= 0) {
            return FluidStack.EMPTY;
        }

        // Определяем тип жидкости из первого источника: смешивать разные
        // жидкости в одном ответе нельзя.
        FluidStack first = proxyTargets.get(0).drain(want, IFluidHandler.FluidAction.SIMULATE);
        if (first.isEmpty()) {
            return FluidStack.EMPTY;
        }

        int extracted = 0;
        for (int i = 0; i < proxyTargets.size() && extracted < want; i++) {
            FluidStack here = proxyTargets.get(i).drain(want - extracted, action);
            extracted += here.getAmount();
        }
        if (extracted <= 0) {
            return FluidStack.EMPTY;
        }

        FluidStack result = new FluidStack(first.getFluid(), extracted, first.getTag());
        setVisual(result);
        applyVisual(level);
        wake();
        return result;
    }

    /** Узнать, какая жидкость переносится сетью (для визуализации/совместимости). */
    public FluidStack proxyGetFluid() {
        return visualFluid;
    }
}