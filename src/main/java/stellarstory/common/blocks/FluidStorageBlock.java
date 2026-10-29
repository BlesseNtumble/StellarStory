package stellarstory.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;
import stellarstory.common.blocks.blockentity.FluidStorageBlockEntity;

/** Жидкостное хранилище. */
public class FluidStorageBlock extends AbstractEnergyBlock {

    public FluidStorageBlock() {
        super(Properties.of()
                .mapColor(MapColor.WATER)
                .strength(2.0F, 6.0F)
                .sound(SoundType.GLASS)
                .noOcclusion());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidStorageBlockEntity(pos, state);
    }
}