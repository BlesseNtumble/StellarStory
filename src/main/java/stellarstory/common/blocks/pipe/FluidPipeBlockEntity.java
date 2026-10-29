package stellarstory.common.blocks.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import stellarstory.common.config.StellarGeneralConfig;
import stellarstory.common.init.StellarBlockEntities;
import stellarstory.common.transfer.network.FluidNetwork;
import stellarstory.common.transfer.network.PipeNetworkManager;

/**
 * BlockEntity жидкостной трубы: визуализация + прозрачный proxy-переходник.
 *
 * renderedFluid — то, что рисует FluidPipeRenderer.
 * Capability (IFluidHandler) — делегирует сети через proxyFill/proxyDrain,
 * чтобы чужие трубы/машины могли подключаться к нашей трубе.
 *
 * Прокси делается на сторону: иначе труба вернула бы жидкость обратно в ту же
 * машину, из которой получила (цикл A→pipe→A).
 */
public class FluidPipeBlockEntity extends BlockEntity {

    private static final String TAG_RENDER_FLUID = "RenderFluid";

    /** Direction.values() клонируется на каждый вызов — держим один массив. */
    private static final Direction[] DIRS = Direction.values();

    /**
     * Защита от рекурсии: чужая машина внутри нашего proxy может тут же
     * обратиться к этой же трубе. Повторный вход запрещаем, иначе StackOverflow.
     */
    private boolean inProxy = false;

    @NotNull
    private FluidStack renderedFluid = FluidStack.EMPTY;

    @SuppressWarnings("unchecked")
    private final LazyOptional<IFluidHandler>[] caps = new LazyOptional[DIRS.length + 1];

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(StellarBlockEntities.FLUID_PIPE.get(), pos, state);
    }

    // ───────────────────────────────────────────────────────────
    // Визуализация
    // ───────────────────────────────────────────────────────────

    public @NotNull FluidStack getRenderedFluid() {
        return renderedFluid;
    }

    public void setRenderedFluid(@Nullable FluidStack fluid) {
        FluidStack next = fluid == null || fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy();
        if (sameFluid(renderedFluid, next)) {
            return;
        }
        renderedFluid = next;
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ───────────────────────────────────────────────────────────
    // Capability (sided proxy)
    // ───────────────────────────────────────────────────────────

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCap(side).cast();
        }
        return super.getCapability(cap, side);
    }

    private LazyOptional<IFluidHandler> fluidCap(@Nullable Direction side) {
        int index = side == null ? DIRS.length : side.ordinal();
        LazyOptional<IFluidHandler> cached = caps[index];
        if (cached == null) {
            cached = LazyOptional.of(() -> new SidedProxy(side));
            caps[index] = cached;
        }
        return cached;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (int i = 0; i < caps.length; i++) {
            if (caps[i] != null) {
                caps[i].invalidate();
                caps[i] = null;
            }
        }
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        for (int i = 0; i < caps.length; i++) {
            caps[i] = null;
        }
    }

    /** Направляет запрос через сеть. */
    private int routeFill(@Nullable Direction face, FluidStack resource, IFluidHandler.FluidAction action) {
        if (resource.isEmpty() || level == null || level.isClientSide || inProxy) {
            return 0;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        PipeNetworkManager manager = PipeNetworkManager.find(serverLevel);
        if (manager == null) return 0;
        if (!(manager.getNetworkAt(worldPosition) instanceof FluidNetwork network)) {
            return 0;
        }
        inProxy = true;
        try {
            return network.proxyFill(serverLevel, face, resource, action);
        } finally {
            inProxy = false;
        }
    }

    private FluidStack routeDrain(@Nullable Direction face, int maxDrain, IFluidHandler.FluidAction action) {
        if (maxDrain <= 0 || level == null || level.isClientSide || inProxy) {
            return FluidStack.EMPTY;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        PipeNetworkManager manager = PipeNetworkManager.find(serverLevel);
        if (manager == null) return FluidStack.EMPTY;
        if (!(manager.getNetworkAt(worldPosition) instanceof FluidNetwork network)) {
            return FluidStack.EMPTY;
        }
        inProxy = true;
        try {
            return network.proxyDrain(serverLevel, face, maxDrain, action);
        } finally {
            inProxy = false;
        }
    }

    /** Ограничитель поверх сети: отдаём наружу только пропускную способность. */
    private final class SidedProxy implements IFluidHandler {

        @Nullable
        private final Direction side;

        private SidedProxy(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            if (level == null || level.isClientSide) {
                return renderedFluid;
            }
            ServerLevel serverLevel = (ServerLevel) level;
            PipeNetworkManager manager = PipeNetworkManager.find(serverLevel);
            if (manager != null) {
                var net = manager.getNetworkAt(worldPosition);
                if (net instanceof FluidNetwork fn) {
                    return fn.proxyGetFluid();
                }
            }
            return renderedFluid;
        }

        @Override
        public int getTankCapacity(int tank) {
            // Показываем пропускную способность, а не MAX_VALUE — иначе чужие трубы
            // будут бесконечно лить, получая 0.
            return StellarGeneralConfig.PIPE_MAX_MB_PER_TICK.get();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return !stack.isEmpty();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return routeFill(side, resource, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return routeDrain(side, resource.getAmount(), action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return routeDrain(side, maxDrain, action);
        }
    }

    // ───────────────────────────────────────────────────────────
    // NBT + синхронизация
    // ───────────────────────────────────────────────────────────

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        writeSyncData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag) {
        readSyncData(tag);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        // Труба не хранит жидкость — после перезагрузки граф сам выставит визуал.
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        renderedFluid = FluidStack.EMPTY;
    }

    private void writeSyncData(@NotNull CompoundTag tag) {
        if (!renderedFluid.isEmpty()) {
            tag.put(TAG_RENDER_FLUID, renderedFluid.writeToNBT(new CompoundTag()));
        }
    }

    private void readSyncData(@NotNull CompoundTag tag) {
        if (tag.contains(TAG_RENDER_FLUID)) {
            renderedFluid = FluidStack.loadFluidStackFromNBT(tag.getCompound(TAG_RENDER_FLUID));
        } else {
            renderedFluid = FluidStack.EMPTY;
        }
    }

    private static boolean sameFluid(@Nullable FluidStack left, @Nullable FluidStack right) {
        if (left == null || left.isEmpty()) {
            return right == null || right.isEmpty();
        }
        return right != null && !right.isEmpty() && left.isFluidEqual(right);
    }
}
