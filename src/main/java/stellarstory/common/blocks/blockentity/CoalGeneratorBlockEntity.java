package stellarstory.common.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.NotNull;
import stellarstory.api.energy.EnergyConfig;
import stellarstory.api.transfer.SideConfig;
import stellarstory.api.transfer.SlotRole;
import stellarstory.common.init.StellarBlockEntities;

/**
 * Генератор на угле: сжигает топливо из верхнего слота и копит FE.
 * Наружу энергия уходит пассивно — её вытягивает сеть труб, поэтому
 * активной перекачки здесь нет (иначе было бы два конкурирующих пути).
 */
public class CoalGeneratorBlockEntity extends AbstractEnergyBlockEntity {

    public static final int CAPACITY = 20_000;
    public static final int MAX_EXTRACT = 500;

    /** FE, производимые за тик горящего топлива. */
    public static final int ENERGY_PER_TICK = 40;

    private static final int FUEL = 0;
    private static final SlotRole[] ROLES = {SlotRole.FUEL};

    private int burnTime = 0;
    private int burnTimeTotal = 0;

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(StellarBlockEntities.COAL_GENERATOR.get(), pos, state,
                EnergyConfig.provider(CAPACITY, MAX_EXTRACT),
                SideConfig.generatorDefaults(),
                ROLES);
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }

        boolean wasBurning = burnTime > 0;

        if (burnTime > 0) {
            burnTime--;
            produceEnergy(ENERGY_PER_TICK);
        } else if (energy < energyConfig.capacity()) {
            int burnTicks = burnTicks(stackInSlot(FUEL));
            if (burnTicks > 0) {
                burnTime = burnTimeTotal = burnTicks;
                inventory.extractItem(FUEL, 1, false);
            }
        }

        if (wasBurning != (burnTime > 0)) {
            syncBurningState();
        }
    }

    /**
     * Тики горения. Важно: Forge-расширение ItemStack#getBurnTime — это только
     * хук для модов и возвращает -1 на ванильном топливе. Реальные значения
     * (уголь=1600) хранятся в FurnaceBlockEntity.getFuel() и подмешиваются
     * исключительно внутри ForgeHooks.getBurnTime, поэтому зовём его напрямую.
     */
    private static int burnTicks(ItemStack stack) {
        return stack.isEmpty() ? 0 : ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    /**
     * Топливо принимает только то, что реально горит, — иначе игрок забьёт слот
     * камнем и будет гадать, почему генератор молчит.
     */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == FUEL && burnTicks(stack) > 0;
    }

    private void syncBurningState() {
        BlockState current = getBlockState();
        if (current.hasProperty(stellarstory.common.blocks.AbstractEnergyBlock.RUNNING)
                && current.getValue(stellarstory.common.blocks.AbstractEnergyBlock.RUNNING) != (burnTime > 0)) {
            level.setBlock(worldPosition,
                    current.setValue(stellarstory.common.blocks.AbstractEnergyBlock.RUNNING, burnTime > 0),
                    Block.UPDATE_CLIENTS);
        }
    }

    /** Прогресс сгорания 0..1 для будущего GUI. */
    public float getBurnProgress() {
        return burnTimeTotal <= 0 ? 0f : (float) burnTime / burnTimeTotal;
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeTotal", burnTimeTotal);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        burnTime = Math.max(0, tag.getInt("BurnTime"));
        burnTimeTotal = Math.max(0, tag.getInt("BurnTimeTotal"));
    }
}
