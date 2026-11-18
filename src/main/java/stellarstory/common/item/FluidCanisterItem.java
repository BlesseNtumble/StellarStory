package stellarstory.common.item;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Предметная канистра для жидкости.
 *
 * Жидкость хранится в NBT стака, поэтому предмет переживает сохранение,
 * копируется и переносится между сторонами без отдельного BlockEntity.
 * Доступ для чужих машин — через стандартный FLUID_HANDLER capability стака.
 */
public class FluidCanisterItem extends Item {

    public static final int CAPACITY = 16_000;
    public static final int MAX_TRANSFER = 1_000;

    public static final String TAG_FLUID = "Fluid";

    public FluidCanisterItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public @NotNull ICapabilityProvider initCapabilities(@NotNull ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidItemCapabilities(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        FluidStack fluid = readFluid(stack);
        if (fluid.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.stellarstory.fluid_empty", CAPACITY));
        } else {
            tooltip.add(Component.translatable("tooltip.stellarstory.fluid_stored",
                    fluid.getDisplayName(), fluid.getAmount(), CAPACITY));
        }
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (be == null) {
            return InteractionResult.PASS;
        }

        Direction side = context.getClickedFace();
        IFluidHandler handler = be.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
        if (handler == null) {
            return InteractionResult.PASS;
        }

        FluidStack held = readFluid(stack);
        if (held.isEmpty()) {
            FluidStack drained = handler.drain(MAX_TRANSFER, IFluidHandler.FluidAction.SIMULATE);
            if (drained.isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                FluidStack executed = handler.drain(drained, IFluidHandler.FluidAction.EXECUTE);
                writeFluid(stack, executed);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        FluidStack request = held.copy();
        request.setAmount(Math.min(MAX_TRANSFER, request.getAmount()));
        int accepted = handler.fill(request, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            FluidStack toFill = held.copy();
            toFill.setAmount(accepted);
            int filled = handler.fill(toFill, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                writeFluid(stack, drainFromStack(stack, filled));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static FluidStack readFluid(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_FLUID)) {
            return FluidStack.EMPTY;
        }
        return FluidStack.loadFluidStackFromNBT(stack.getTag().getCompound(TAG_FLUID));
    }

    public static void writeFluid(ItemStack stack, @Nullable FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) {
            if (stack.hasTag()) {
                stack.getTag().remove(TAG_FLUID);
            }
            return;
        }
        stack.getOrCreateTag().put(TAG_FLUID, fluid.writeToNBT(new CompoundTag()));
    }

    private static FluidStack drainFromStack(ItemStack stack, int amount) {
        FluidStack held = readFluid(stack);
        if (held.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int left = held.getAmount() - Math.max(0, amount);
        if (left <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack result = held.copy();
        result.setAmount(left);
        return result;
    }

    /**
     * Capability жидкости стака. Провайдер создаётся на каждый ItemStack и читает
     * NBT в момент обращения, поэтому copy()/клонирование стака переносят содержимое корректно.
     */
    private final class FluidItemCapabilities implements ICapabilityProvider {

        private final ItemStack owner;
        private final LazyOptional<IFluidHandler> lazy;

        private FluidItemCapabilities(ItemStack owner) {
            this.owner = owner;
            this.lazy = LazyOptional.of(() -> new ItemFluid(owner));
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            // Канистра — и бак, и предмет-контейнер: отдаём оба capability одному хендлеру,
            // чтобы машины (FluidStorage) работали с ней через стандартный FLUID_HANDLER_ITEM.
            if (cap == ForgeCapabilities.FLUID_HANDLER || cap == ForgeCapabilities.FLUID_HANDLER_ITEM) {
                return lazy.cast();
            }
            return LazyOptional.empty();
        }
    }

    private static final class ItemFluid implements IFluidHandlerItem {

        private final ItemStack stack;

        private ItemFluid(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public @NotNull ItemStack getContainer() {
            return stack;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return readFluid(stack);
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return true;
        }

        @Override
        public int fill(@NotNull FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            FluidStack stored = readFluid(stack);
            if (!stored.isEmpty() && !stored.isFluidEqual(resource)) {
                return 0;
            }
            int accepted = Math.min(resource.getAmount(), CAPACITY - stored.getAmount());
            if (accepted <= 0) {
                return 0;
            }
            if (action.execute()) {
                FluidStack next = resource.copy();
                next.setAmount(stored.getAmount() + accepted);
                writeFluid(stack, next);
            }
            return accepted;
        }

        @Override
        public @NotNull FluidStack drain(@NotNull FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            FluidStack stored = readFluid(stack);
            if (stored.isEmpty() || !stored.isFluidEqual(resource)) {
                return FluidStack.EMPTY;
            }
            int extracted = Math.min(stored.getAmount(), resource.getAmount());
            if (extracted <= 0) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                writeFluid(stack, drainFromStack(stack, extracted));
            }
            FluidStack result = stored.copy();
            result.setAmount(extracted);
            return result;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            FluidStack stored = readFluid(stack);
            if (stored.isEmpty()) {
                return FluidStack.EMPTY;
            }
            int extracted = Math.min(stored.getAmount(), maxDrain);
            if (extracted <= 0) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                writeFluid(stack, drainFromStack(stack, extracted));
            }
            FluidStack result = stored.copy();
            result.setAmount(extracted);
            return result;
        }
    }
}