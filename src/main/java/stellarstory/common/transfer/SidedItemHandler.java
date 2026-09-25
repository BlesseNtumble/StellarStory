package stellarstory.common.transfer;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import stellarstory.api.transfer.SideConfig;
import stellarstory.api.transfer.SideMode;
import stellarstory.api.transfer.SlotRole;
import stellarstory.api.transfer.TransferKind;

import java.util.Arrays;

/**
 * Ограничитель доступа к инвентарю со стороны.
 *
 * Роли сторон читаются ЖИВОМ при каждом вызове, поэтому LazyOptional на capability
 * кэшируется один раз на пару (BlockEntity, сторона) и не требует инвалидации
 * при смене настройки игроком — сбрасывается только карта видимых слотов.
 */
public class SidedItemHandler implements IItemHandlerModifiable {

    private static final int[] EMPTY = new int[0];

    private final IItemHandlerModifiable inner;
    /** null = внутренний доступ без ограничений. */
    private final Direction side;
    private final SideConfig sideConfig;
    private final SlotRole[] slotRoles;

    /** Карта "номер слота у соседа -> реальный слот". Пересобирается только при смене настройки. */
    private int[] view;

    public SidedItemHandler(IItemHandlerModifiable inner, Direction side, SideConfig sideConfig, SlotRole[] slotRoles) {
        this.inner = inner;
        this.side = side;
        this.sideConfig = sideConfig;
        this.slotRoles = slotRoles;
        refresh();
    }

    /** Вызывать после изменения SideConfig. Не дёргать каждый тик. */
    public void refresh() {
        boolean in = sideConfig.canReceive(TransferKind.ITEM, side);
        boolean out = sideConfig.canExtract(TransferKind.ITEM, side);
        int[] tmp = new int[slotRoles.length];
        int n = 0;
        for (int i = 0; i < slotRoles.length; i++) {
            SlotRole role = slotRoles[i];
            if (role == SlotRole.INTERNAL) {
                continue;
            }
            if ((in && role.insertable) || (out && role.extractable)) {
                tmp[n++] = i;
            }
        }
        view = n == 0 ? EMPTY : Arrays.copyOf(tmp, n);
    }

    private int real(int slot) {
        return slot >= 0 && slot < view.length ? view[slot] : -1;
    }

    @Override
    public int getSlots() {
        return view.length;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        int real = real(slot);
        return real < 0 ? ItemStack.EMPTY : inner.getStackInSlot(real);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        int real = real(slot);
        if (real < 0 || !slotRoles[real].insertable) {
            return stack;
        }
        SideMode mode = side == null ? SideMode.BOTH : sideConfig.get(TransferKind.ITEM, side);
        if (!mode.canReceive()) {
            return stack;
        }
        return inner.insertItem(real, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        int real = real(slot);
        if (real < 0 || !slotRoles[real].extractable) {
            return ItemStack.EMPTY;
        }
        SideMode mode = side == null ? SideMode.BOTH : sideConfig.get(TransferKind.ITEM, side);
        if (!mode.canExtract()) {
            return ItemStack.EMPTY;
        }
        return inner.extractItem(real, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        int real = real(slot);
        return real < 0 ? 0 : inner.getSlotLimit(real);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        int real = real(slot);
        return real >= 0 && inner.isItemValid(real, stack);
    }

    /**
     * Прямая запись извне запрещена: иначе сосед мог бы подменить содержимое
     * выходного слота. Внутренняя логика машины работает с ItemStackHandler напрямую.
     */
    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        // намеренно пусто
    }
}
