package stellarstory.common.blocks.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import stellarstory.common.config.StellarGeneralConfig;
import stellarstory.common.init.StellarBlockEntities;
import stellarstory.common.transfer.network.EnergyNetwork;
import stellarstory.common.transfer.network.PipeNetworkManager;

public class EnergyPipeBlockEntity extends BlockEntity {

    private static final Direction[] DIRS = Direction.values();

    @SuppressWarnings("unchecked")
    private final LazyOptional<IEnergyStorage>[] caps = new LazyOptional[DIRS.length + 1];

    private boolean inProxy = false;

    public EnergyPipeBlockEntity(BlockPos pos, BlockState state) {
        super(StellarBlockEntities.ENERGY_PIPE.get(), pos, state);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap(side).cast();
        }
        return super.getCapability(cap, side);
    }

    private LazyOptional<IEnergyStorage> energyCap(@Nullable Direction side) {
        int index = side == null ? DIRS.length : side.ordinal();
        LazyOptional<IEnergyStorage> cached = caps[index];
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

    private int route(@Nullable Direction face, int amount, boolean extract, boolean simulate) {
        if (level == null || level.isClientSide || amount <= 0 || inProxy) {
            return 0;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        PipeNetworkManager manager = PipeNetworkManager.find(serverLevel);
        if (manager == null) {
            return 0;
        }
        if (!(manager.getNetworkAt(worldPosition) instanceof EnergyNetwork network)) {
            return 0;
        }
        inProxy = true;
        try {
            return extract
                    ? network.proxyExtract(serverLevel, face, amount, simulate)
                    : network.proxyReceive(serverLevel, face, amount, simulate);
        } finally {
            inProxy = false;
        }
    }

    private final class SidedProxy implements IEnergyStorage {

        @Nullable
        private final Direction side;

        private SidedProxy(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return route(side, maxReceive, false, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return route(side, maxExtract, true, simulate);
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return StellarGeneralConfig.PIPE_MAX_FE_PER_TICK.get();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
