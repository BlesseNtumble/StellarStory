package stellarstory.common.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import stellarstory.api.energy.EnergyConfig;
import stellarstory.api.transfer.SideConfig;
import stellarstory.common.init.StellarBlockEntities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Жидкостный инфузер: тратит предмет и жидкость, выдаёт новый предмет.
 *
 * Рецепты пока статические — как в Дробителе. Правильный путь на будущее:
 * собственный RecipeType + JSON в датапаках.
 */
public class FluidInfuserBlockEntity extends AbstractMachineBlockEntity {

    public static final int CAPACITY = 10_000;
    public static final int MAX_RECEIVE = 200;
    public static final int ENERGY_PER_TICK = 25;
    public static final int OPERATE_TIME = 100;
    public static final int TANK_CAPACITY = 16_000;
    public static final int FLUID_PER_OPERATION = 1_000;

    private static final int INPUT = 0;
    private static final int OUTPUT = 1;

    private static final Map<RecipeKey, Recipe> RECIPES = new HashMap<>();

    static {
        add(Items.COBBLESTONE, Fluids.WATER, new ItemStack(Items.STONE));
        add(Items.SAND, Fluids.WATER, new ItemStack(Items.CLAY));
        add(Items.GRAVEL, Fluids.LAVA, new ItemStack(Items.MAGMA_BLOCK));
        add(Items.CLAY, Fluids.LAVA, new ItemStack(Items.BRICKS));
        add(Items.IRON_INGOT, Fluids.LAVA, new ItemStack(Items.NETHERITE_SCRAP));
    }

    private static void add(Item input, Fluid fluid, ItemStack output) {
        RECIPES.put(new RecipeKey(input, fluid), new Recipe(input, fluid, output, FLUID_PER_OPERATION));
    }

    public FluidInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(StellarBlockEntities.FLUID_INFUSER.get(), pos, state,
                EnergyConfig.receiver(CAPACITY, MAX_RECEIVE),
                SideConfig.fluidMachineDefaults(),
                2,
                TANK_CAPACITY);
    }

    @Override
    protected int getEnergyPerTick() {
        return ENERGY_PER_TICK;
    }

    @Override
    protected int getOperateTime() {
        return OPERATE_TIME;
    }

    @Nullable
    private Recipe currentRecipe() {
        ItemStack input = stackInSlot(INPUT);
        if (input.isEmpty()) {
            return null;
        }
        FluidStack fluid = getFluidInTank(0);
        if (fluid.isEmpty()) {
            return null;
        }
        return RECIPES.get(new RecipeKey(input.getItem(), fluid.getFluid()));
    }

    @Override
    protected boolean canProcess() {
        Recipe recipe = currentRecipe();
        if (recipe == null) {
            return false;
        }
        if (getFluidAmount(0) < recipe.fluidAmount) {
            return false;
        }
        return fitsOutput(recipe.output);
    }

    @Override
    protected void finishProgress() {
        Recipe recipe = currentRecipe();
        if (recipe == null || !canProcess()) {
            return;
        }
        inventory.extractItem(INPUT, 1, false);
        getTank(0).drain(recipe.fluidAmount, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        insertResult(OUTPUT, recipe.output);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, @NotNull ItemStack stack) {
        if (slot != INPUT || stack.isEmpty()) {
            return slot == INPUT;
        }
        return hasRecipeFor(stack.getItem());
    }

    private static boolean hasRecipeFor(Item item) {
        for (RecipeKey key : RECIPES.keySet()) {
            if (key.item == item) {
                return true;
            }
        }
        return false;
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

    public static List<Item> getValidInputs() {
        List<Item> items = new ArrayList<>();
        for (RecipeKey key : RECIPES.keySet()) {
            if (!items.contains(key.item)) {
                items.add(key.item);
            }
        }
        return items;
    }

    private record RecipeKey(Item item, Fluid fluid) {
    }

    private record Recipe(Item input, Fluid fluid, ItemStack output, int fluidAmount) {
    }
}