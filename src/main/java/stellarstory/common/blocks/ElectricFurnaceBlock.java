package stellarstory.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;
import stellarstory.common.blocks.blockentity.ElectricFurnaceBlockEntity;

/** Электропечь. */
public class ElectricFurnaceBlock extends AbstractEnergyBlock {

    public ElectricFurnaceBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(4.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricFurnaceBlockEntity(pos, state);
    }
}
