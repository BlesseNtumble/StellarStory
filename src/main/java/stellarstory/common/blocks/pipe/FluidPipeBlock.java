package stellarstory.common.blocks.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import stellarstory.api.transfer.TransferKind;

/** Жидкостная труба: сегмент жидкостной сети с визуализацией переносимой жидкости. */
public class FluidPipeBlock extends AbstractPipeBlock {

    public FluidPipeBlock() {
        super(Properties.of()
                .mapColor(MapColor.WATER)
                .strength(2.0F, 4.0F)
                .sound(SoundType.GLASS));
    }

    @Override
    protected TransferKind getTransferKind() {
        return TransferKind.FLUID;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    /**
     * Форма трубы остаётся упрощённой: рендер жидкости делает BlockEntity,
     * а коллизия не должна перекрывать прозрачные грани модели.
     */
    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                                 @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.empty();
    }
}