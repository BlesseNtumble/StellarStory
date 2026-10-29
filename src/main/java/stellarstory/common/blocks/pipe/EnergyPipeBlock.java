package stellarstory.common.blocks.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;
import stellarstory.api.transfer.TransferKind;
import stellarstory.common.init.StellarBlockEntities;

/** Энерготруба: сегмент энергосети модели A (шина без собственной ёмкости). */
public class EnergyPipeBlock extends AbstractPipeBlock {

    public EnergyPipeBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0F, 4.0F)
                .sound(SoundType.METAL));
    }

    @Override
    protected TransferKind getTransferKind() {
        return TransferKind.ENERGY;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyPipeBlockEntity(pos, state);
    }
}
