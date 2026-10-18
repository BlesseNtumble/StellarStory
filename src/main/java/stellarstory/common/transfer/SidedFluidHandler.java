package stellarstory.common.transfer;

import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import stellarstory.api.transfer.SideConfig;
import stellarstory.api.transfer.TransferKind;

import java.util.List;

/**
 * Ограничитель доступа к танкам жидкости со стороны через {@link SideConfig}.
 *
 * Показывает соседу только те танки, чей индекс входит в переданный список,
 * и блокирует fill/drain, если роль грани не разрешает операцию.
 * null-сторона = внутренний доступ без ограничений.
 */
public class SidedFluidHandler implements IFluidHandler {

    private final List<FluidTank> tanks;
    private final Direction side;
    private final SideConfig sideConfig;

    public SidedFluidHandler(List<FluidTank> tanks, @javax.annotation.Nullable Direction side, SideConfig sideConfig) {
        this.tanks = tanks;
        this.side = side;
        this.sideConfig = sideConfig;
    }

    @Override
    public int getTanks() {
        return tanks.size();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return tanks.get(tank).getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return tanks.get(tank).getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return tanks.get(tank).isFluidValid(stack);
    }

    @Override
    public int fill(@NotNull FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !sideConfig.canReceive(TransferKind.FLUID, side)) {
            return 0;
        }
        int total = 0;
        for (FluidTank tank : tanks) {
            total += tank.fill(resource.copy(), FluidAction.SIMULATE);
            if (total >= resource.getAmount()) {
                break;
            }
        }
        int accepted = Math.min(total, resource.getAmount());
        if (accepted <= 0) {
            return 0;
        }
        if (action.execute()) {
            int left = accepted;
            for (FluidTank tank : tanks) {
                if (left <= 0) {
                    break;
                }
                FluidStack part = resource.copy();
                part.setAmount(left);
                left -= tank.fill(part, FluidAction.EXECUTE);
            }
        }
        return accepted;
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !sideConfig.canExtract(TransferKind.FLUID, side)) {
            return FluidStack.EMPTY;
        }
        for (FluidTank tank : tanks) {
            FluidStack stored = tank.getFluid();
            if (!stored.isEmpty() && stored.isFluidStackIdentical(resource)) {
                return tank.drain(resource, action);
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0 || !sideConfig.canExtract(TransferKind.FLUID, side)) {
            return FluidStack.EMPTY;
        }
        for (FluidTank tank : tanks) {
            if (!tank.isEmpty()) {
                return tank.drain(maxDrain, action);
            }
        }
        return FluidStack.EMPTY;
    }
}
