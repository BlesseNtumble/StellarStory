package stellarstory.common.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import stellarstory.api.energy.EnergyConfig;
import stellarstory.api.transfer.SideConfig;
import stellarstory.common.init.StellarBlockEntities;

import java.util.HashMap;
import java.util.Map;

/**
 * Дробитель: свой набор рецептов (ЗАГЛУШКА в статической таблице).
 * Правильный вариант — собственный RecipeType + JSON в датапаках;
 * таблица здесь затем, чтобы цепочка энергия -> машина была рабочей уже сейчас.
 */
public class GrinderBlockEntity extends AbstractMachineBlockEntity {

    public static final int CAPACITY = 10_000;
    public static final int MAX_RECEIVE = 200;
    public static final int ENERGY_PER_TICK = 30;
    public static final int OPERATE_TIME = 160;

    private static final int INPUT = 0;
    private static final int OUTPUT = 1;

    private static final Map<Item, ItemStack> RECIPES = new HashMap<>();

    static {
        RECIPES.put(Items.COBBLESTONE, new ItemStack(Items.GRAVEL));
        RECIPES.put(Items.GRAVEL, new ItemStack(Items.SAND));
        RECIPES.put(Items.RAW_IRON, new ItemStack(Items.RAW_IRON, 2));
        RECIPES.put(Items.RAW_COPPER, new ItemStack(Items.RAW_COPPER, 2));
        RECIPES.put(Items.RAW_GOLD, new ItemStack(Items.RAW_GOLD, 2));
    }

    public GrinderBlockEntity(BlockPos pos, BlockState state) {
        super(StellarBlockEntities.GRINDER.get(), pos, state,
                EnergyConfig.receiver(CAPACITY, MAX_RECEIVE),
                SideConfig.machineDefaults(),
                2);
    }

    @Override
    protected int getEnergyPerTick() {
        return ENERGY_PER_TICK;
    }

    @Override
    protected int getOperateTime() {
        return OPERATE_TIME;
    }

    private ItemStack resultFor(ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = RECIPES.get(input.getItem());
        return result == null ? ItemStack.EMPTY : result.copy();
    }

    @Override
    protected boolean canProcess() {
        ItemStack result = resultFor(stackInSlot(INPUT));
        if (result.isEmpty()) {
            return false;
        }
        ItemStack out = stackInSlot(OUTPUT);
        return out.isEmpty() || (out.is(result.getItem())
                && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    @Override
    protected void finishProgress() {
        ItemStack result = resultFor(stackInSlot(INPUT));
        if (result.isEmpty()) {
            return;
        }
        inventory.extractItem(INPUT, 1, false);
        insertResult(OUTPUT, result);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == INPUT && RECIPES.containsKey(stack.getItem());
    }
}
