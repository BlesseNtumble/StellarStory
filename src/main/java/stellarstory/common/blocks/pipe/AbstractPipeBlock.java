package stellarstory.common.blocks.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import stellarstory.api.transfer.ISideConfigurable;
import stellarstory.api.transfer.SideMode;
import stellarstory.api.transfer.TransferKind;
import stellarstory.common.transfer.network.PipeNetworkManager;

import java.util.Map;

/**
 * Труба без BlockEntity: состояние держит граф в PipeNetworkManager,
 * а соединения — свойства блока (флаг на каждую грань).
 *
 * Модель собирается из ядра и рукавов по граням с connection=true — как в IC2,
 * но средствами ванильных блокстейтов: multipart, клиентского кода нет.
 *
 * В 1.20.1 нет блочного хука getCapability(BlockState, BlockGetter, BlockPos, Direction)
 * (он появился в 1.20.2), поэтому труба не отдаёт capability на своей позиции:
 * сеть сама ходит по эндпоинтам-машинам. Следствие — чужой мод не может
 * «затолкнуть» энергию в нашу трубу; прием/отдача между нашими блоками работают.
 */
public abstract class AbstractPipeBlock extends Block {

    private static final Direction[] DIRS = Direction.values();

    /** Ядро и рукава: 8/16 поперёк у ядра, 6/16 у рукава. */
    private static final VoxelShape CORE = box(4, 4, 4, 12, 12, 12);
    private static final VoxelShape ARM_DOWN = box(5, 0, 5, 11, 4, 11);
    private static final VoxelShape ARM_UP = box(5, 12, 5, 11, 16, 11);
    private static final VoxelShape ARM_NORTH = box(5, 5, 0, 11, 11, 4);
    private static final VoxelShape ARM_SOUTH = box(5, 5, 12, 11, 11, 16);
    private static final VoxelShape ARM_WEST = box(0, 5, 5, 4, 11, 11);
    private static final VoxelShape ARM_EAST = box(12, 5, 5, 16, 11, 11);

    /**
     * Форма кэшируется на каждый из 64 состояний один раз при создании блока:
     * getShape вызывается рендером на каждый видимый блок, пересобирать VoxelShape
     * в нём нельзя.
     */
    private final Map<BlockState, VoxelShape> shapes;

    protected AbstractPipeBlock(Properties properties) {
        // noOcclusion: рукава тонкие, сосед должен рендерить свою грань против трубы.
        super(properties.noOcclusion());
        this.shapes = getShapeForEachState(this::shapeFor);
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        // Порядок объявления совпадает с алфавитом — ключи blockstates однозначны.
        builder.add(BlockStateProperties.DOWN, BlockStateProperties.EAST, BlockStateProperties.NORTH,
                BlockStateProperties.SOUTH, BlockStateProperties.UP, BlockStateProperties.WEST);
    }

    @Override
    public @NotNull BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    /** Хук ванильных shape-обновлений: генерация мира, структуры, поршни. */
    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction,
                                           @NotNull BlockState neighborState, @NotNull LevelAccessor level,
                                           @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        return withConnections(state, level, pos);
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                        @NotNull BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        PipeNetworkManager.get(level).onPipePlaced(pos);
        refreshConnections(level, pos);
        refreshPipeNeighbors(level, pos);
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         @NotNull BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            PipeNetworkManager.get(level).onPipeRemoved(pos);
            // Соединения соседних труб починит neighborChanged — он приходит на разрыв.
        }
    }

    /**
     * Реакция на появление/исчезновение блока рядом: граф помечается грязным,
     * а визуальные соединения пересобираются — сосед-труба и машина одинаково
     * меняют форму трубы.
     */
    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                @NotNull Block neighborBlock, @NotNull BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        PipeNetworkManager.get(level).onNeighborChanged(pos, neighborBlock);
        refreshConnections(level, pos);
    }

    // ═══════════════════════════════════════════════════════════════
    // Форма
    // ═══════════════════════════════════════════════════════════════

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return shapes.getOrDefault(state, CORE);
    }

    /** Труба не мешает ходить — как в IC2. */
    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                                 @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.empty();
    }

    private VoxelShape shapeFor(BlockState state) {
        VoxelShape shape = CORE;
        for (int i = 0; i < DIRS.length; i++) {
            if (state.getValue(propertyFor(DIRS[i]))) {
                shape = Shapes.or(shape, armFor(DIRS[i]));
            }
        }
        return shape;
    }

    private static VoxelShape armFor(Direction dir) {
        return switch (dir) {
            case DOWN -> ARM_DOWN;
            case UP -> ARM_UP;
            case NORTH -> ARM_NORTH;
            case SOUTH -> ARM_SOUTH;
            case WEST -> ARM_WEST;
            case EAST -> ARM_EAST;
        };
    }

    private static BooleanProperty propertyFor(Direction dir) {
        return switch (dir) {
            case DOWN -> BlockStateProperties.DOWN;
            case UP -> BlockStateProperties.UP;
            case NORTH -> BlockStateProperties.NORTH;
            case SOUTH -> BlockStateProperties.SOUTH;
            case WEST -> BlockStateProperties.WEST;
            case EAST -> BlockStateProperties.EAST;
        };
    }

    // ═══════════════════════════════════════════════════════════════
    // Соединения
    // ═══════════════════════════════════════════════════════════════

    /**
     * Труба соединяется со своей трубой (труба другого типа — другая сеть, рисовать
     * мост между ними значит врать игроку) и с блоками, у которых есть BlockEntity:
     * машины, сундуки, хопперы.
     *
     * if (sideConfigurable) — проверяем режим стороны: DISABLED = соединения нет.
     */
    protected boolean canConnectTo(@NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.isAir()) {
            return false;
        }
        if (neighbor.getBlock() == this) {
            return true;
        }
        if (!neighbor.hasBlockEntity()) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(neighborPos);
        if (be instanceof ISideConfigurable configurable) {
            // Труба подходит к грани машины, обращённой к ней.
            Direction machineFace = direction.getOpposite();
            SideMode mode = configurable.getSideConfig().get(getTransferKind(), machineFace);
            return mode.canReceive() || mode.canExtract();
        }
        return true;
    }

    /** Вид переноса, который переносит эта труба. У энерготрубы — ENERGY. */
    protected abstract TransferKind getTransferKind();

    private BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState result = state;
        for (int i = 0; i < DIRS.length; i++) {
            Direction dir = DIRS[i];
            BooleanProperty property = propertyFor(dir);
            boolean connect = canConnectTo(level, pos, dir);
            if (result.getValue(property) != connect) {
                result = result.setValue(property, connect);
            }
        }
        return result;
    }

    /** UPDATE_KNOWN_SHAPE не уведомляет соседей — обратных вызовов в петлю не возникает. */
    private void refreshConnections(Level level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() != this) {
            return;
        }
        BlockState updated = withConnections(current, level, pos);
        if (updated != current) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /**
     * Ставим трубу — соседние трубы должны получить рукав в нашу сторону.
     * Одного прохода достаточно: соединения соседей относительно их соседей не меняются.
     */
    private void refreshPipeNeighbors(Level level, BlockPos pos) {
        for (int i = 0; i < DIRS.length; i++) {
            BlockPos neighborPos = pos.relative(DIRS[i]);
            if (level.getBlockState(neighborPos).getBlock() == this) {
                refreshConnections(level, neighborPos);
            }
        }
    }
}
