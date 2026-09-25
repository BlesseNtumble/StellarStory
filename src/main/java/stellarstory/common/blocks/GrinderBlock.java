package stellarstory.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;
import stellarstory.common.blocks.blockentity.GrinderBlockEntity;

/** Дробитель. */
public class GrinderBlock extends AbstractEnergyBlock {

    public GrinderBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(4.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrinderBlockEntity(pos, state);
    }
}
