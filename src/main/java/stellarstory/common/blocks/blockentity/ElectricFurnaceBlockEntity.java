package stellarstory.common.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import stellarstory.api.energy.EnergyConfig;
import stellarstory.api.transfer.SideConfig;
import stellarstory.api.transfer.SlotRole;
import stellarstory.common.init.StellarBlockEntities;

import java.util.Optional;

/**
 * Электропечь: рецепт vanilla SMELTING, энергия вместо топлива.
 * 1 вход + 1 выход; выход наружу отдаётся только со сторон в режиме OUTPUT.
 */
public class ElectricFurnaceBlockEntity extends AbstractMachineBlockEntity {

    public static final int CAPACITY = 10_000;
    public static final int MAX_RECEIVE = 200;
    public static final int ENERGY_PER_TICK = 20;
    public static final int OPERATE_TIME = 120;

    private static final int INPUT = 0;
    private static final int OUTPUT = 1;
    private static final SlotRole[] ROLES = {SlotRole.INPUT, SlotRole.OUTPUT};

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(StellarBlockEntities.ELECTRIC_FURNACE.get(), pos, state,
                EnergyConfig.receiver(CAPACITY, MAX_RECEIVE),
                SideConfig.machineDefaults(),
                ROLES);
    }

    @Override
    protected int getEnergyPerTick() {
        return ENERGY_PER_TICK;
    }

    @Override
    protected int getOperateTime() {
        return OPERATE_TIME;
    }

    private Optional<SmeltingRecipe> currentRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        ItemStack input = stackInSlot(INPUT);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(input), level);
    }

    @Override
    protected boolean canProcess() {
        Optional<SmeltingRecipe> recipe = currentRecipe();
        if (recipe.isEmpty()) {
            return false;
        }
        return fitsOutput(recipe.get().getResultItem(level.registryAccess()));
    }

    @Override
    protected void finishProgress() {
        currentRecipe().ifPresent(recipe -> {
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (result.isEmpty() || !fitsOutput(result)) {
                return;
            }
            inventory.extractItem(INPUT, 1, false);
            inventory.insertItem(OUTPUT, result.copy(), false);
        });
    }

    /** Хватает ли места в выходном слоте под результат. */
    private boolean fitsOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack out = stackInSlot(OUTPUT);
        return out.isEmpty() || (out.is(result.getItem())
                && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    /** В вход кладём только то, что реально плавится. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot != INPUT || stack.isEmpty() || level == null) {
            return slot == INPUT;
        }
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), level).isPresent();
    }
}
