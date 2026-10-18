package stellarstory.common.transfer;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import stellarstory.api.transfer.SideConfig;
import stellarstory.api.transfer.SideMode;
import stellarstory.api.transfer.TransferKind;

/**
 * Ограничитель доступа к инвентарю со стороны через {@link SideConfig}.
 *
 * Больше не использует SlotRole — все слоты видны одинаково,
 * ограничения задаются на грань через GUI или гаечный ключ.
 * null-сторона = внутренний доступ без ограничений.
 */
public class SidedItemHandler implements IItemHandlerModifiable {

    private final IItemHandlerModifiable inner;
    private final Direction side;
    private final SideConfig sideConfig;

    public SidedItemHandler(IItemHandlerModifiable inner, Direction side, SideConfig sideConfig) {
        this.inner = inner;
        this.side = side;
        this.sideConfig = sideConfig;
    }

    @Override
    public int getSlots() {
        return inner.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return inner.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        SideMode mode = side == null ? SideMode.BOTH : sideConfig.get(TransferKind.ITEM, side);
        if (!mode.canReceive()) {
            return stack;
        }
        return inner.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        SideMode mode = side == null ? SideMode.BOTH : sideConfig.get(TransferKind.ITEM, side);
        if (!mode.canExtract()) {
            return ItemStack.EMPTY;
        }
        return inner.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inner.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return inner.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        // намеренно пусто — внешняя запись запрещена
    }
}
