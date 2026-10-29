package stellarstory.common.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import stellarstory.api.energy.EnergyConfig;
import stellarstory.api.transfer.SideConfig;
import stellarstory.common.init.StellarBlockEntities;

/**
 * Жидкостное хранилище: два слота для канистр/ведр.
 *
 * Слот 0 — заполнение (забирает жидкость из стака, кладёт в бак).
 * Слот 1 — опорожнение (забирает жидкость из бака, кладёт в стак).
 *
 * При опорожнении проверяется, может ли предмет хранить эту жидкость:
 * - Empty bucket → filled bucket of the same fluid
 * - FluidCanister → filled canister
 * - Любой предмет с FLUID_HANDLER_ITEM → fill via capability
 */
public class FluidStorageBlockEntity extends AbstractEnergyBlockEntity {

    /** Максимум жидкости, перекачиваемой за один тик в одном слоте (1000 мБ = 1 ведро). */
    private static final int TRANSFER_PER_TICK = FluidType.BUCKET_VOLUME;

    public FluidStorageBlockEntity(BlockPos pos, BlockState state) {
        super(StellarBlockEntities.FLUID_STORAGE.get(), pos, state,
                EnergyConfig.of(0, 0, 0),
                SideConfig.fluidStorageDefaults(),
                2,      // 2 слота для канистр/ведр
                32_000); // один бак на 32 000 мБ
    }

    @Override
    public void serverTick() {
        syncFluidIfNeeded();

        if (level == null || level.isClientSide) {
            return;
        }

        // Слот 0: заполнение — берём жидкость из стака и льём в бак
        drainSlotToFillingSlot(0);

        // Слот 1: опорожнение — берём жидкость из бака и кладём в стак
        fillSlotFromDrainSlot(1);
    }

    /**
     * Забрать жидкость из стака в слоте и положить в бак.
     * Работает с любым предметом, имеющим IFluidHandlerItem capability:
     * канистра, ведро (пустое или полное).
     */
    private void drainSlotToFillingSlot(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return;
        }

        IFluidHandlerItem handler = resolveHandlerItem(stack);
        if (handler == null) {
            return;
        }

        FluidTank tank = tanks.get(0);
        int freeSpace = tank.getCapacity() - tank.getFluidAmount();
        if (freeSpace <= 0) {
            return;
        }

        int maxDrain = Math.min(freeSpace, TRANSFER_PER_TICK);
        FluidStack drained = handler.drain(maxDrain, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) {
            return;
        }

        int accepted = tank.fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return;
        }

        // Выполняем: сначала drain, затем fill
        FluidStack executed = handler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        if (executed.isEmpty()) {
            return;
        }
        tank.fill(executed, IFluidHandler.FluidAction.EXECUTE);

        // Обновляем стак в слоте (для ведра — может измениться на empty bucket)
        inventory.setStackInSlot(slot, handler.getContainer());
    }

    /**
     * Забрать жидкость из бака и положить в стак в слоте.
     * При опорожнении проверяется, может ли предмет хранить эту жидкость.
     * Если стак уже содержит жидкость — проверяем совпадение типа.
     */
    private void fillSlotFromDrainSlot(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return;
        }

        FluidTank tank = tanks.get(0);
        FluidStack tankFluid = tank.getFluid();
        if (tankFluid.isEmpty()) {
            return;
        }

        IFluidHandlerItem handler = resolveHandlerItem(stack);
        if (handler == null) {
            return;
        }

        int maxFill = Math.min(tankFluid.getAmount(), TRANSFER_PER_TICK);
        FluidStack request = tankFluid.copy();
        request.setAmount(maxFill);

        // Проверяем: может ли предмет хранить эту жидкость
        int accepted = handler.fill(request, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return;
        }

        // Выполняем: сначала drain из бака, затем fill в стак
        FluidStack drained = tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return;
        }
        handler.fill(drained, IFluidHandler.FluidAction.EXECUTE);

        inventory.setStackInSlot(slot, handler.getContainer());
    }

    /**
     * Получить IFluidHandlerItem из стака.
     * Проверяет FLUID_HANDLER_ITEM capability, а также обрабатывает
     * ванильные BucketItem и нашу FluidCanisterItem.
     */
    @Nullable
    private static IFluidHandlerItem resolveHandlerItem(ItemStack stack) {
        // 1. Capability FLUID_HANDLER_ITEM — стандартный путь для канистры и ведра
        LazyOptional<? extends IFluidHandlerItem> cap = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM, null);
        if (cap.isPresent()) {
            return cap.orElse(null);
        }

        return null;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // В хранилище можно класть любые стакы с FLUID_HANDLER_ITEM capability
        // (канистры, полные/пустые ведра) или пустые стакы (чтобы положить их позже)
        if (stack.isEmpty()) {
            return true;
        }
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM, null).isPresent();
    }

    @Override
    protected void writeSyncData(@NotNull CompoundTag tag) {
        super.writeSyncData(tag);
        tag.putInt("MaxFluid", 32_000);
    }
}