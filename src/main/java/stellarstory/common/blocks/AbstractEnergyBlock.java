package stellarstory.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import stellarstory.common.blocks.blockentity.AbstractEnergyBlockEntity;
import stellarstory.common.blocks.blockentity.IEnergyTickedBlockEntity;

/**
 * Блок-механизм: создаёт BlockEntity, добавляет свойства RUNNING/FACING
 * и серверный тикер. Тикер работает через интерфейс, поэтому новые машины
 * не требуют правки этого класса.
 */
public abstract class AbstractEnergyBlock extends BaseEntityBlock {

    /** Работающий механизм (подсветка/анимация). */
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    /** Горизонтальный поворот — меняется гаечным ключом. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected AbstractEnergyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(RUNNING, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RUNNING, FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Поворот по часовой стрелке (гаечный ключ). */
    public BlockState cycleFacing(BlockState state) {
        return state.setValue(FACING, state.getValue(FACING).getClockWise());
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Ручная загрузка/выгрузка до появления GUI:
     *   ПКМ с предметом  — вставить стак в первый вставляемый слот;
     *   Shift+ПКМ пустой рукой — забрать стак из первого извлекаемого слота.
     * Мутация только на сервере; клиент лишь предсказывает успех для анимации руки.
     */
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand,
                                          @NotNull BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AbstractEnergyBlockEntity machine)) {
            return InteractionResult.PASS;
        }

        boolean emptyHand = player.getItemInHand(hand).isEmpty();

        if (player.isSecondaryUseActive() && emptyHand) {
            if (!level.isClientSide) {
                machine.playerExtract(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!emptyHand) {
            boolean inserted = level.isClientSide
                    ? machine.hasInsertableSlot()
                    : machine.playerInsert(player, hand);
            if (inserted) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                 @NotNull BlockState state,
                                                                 @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (world, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof IEnergyTickedBlockEntity ticked) {
                ticked.serverTick();
            }
        };
    }
}
