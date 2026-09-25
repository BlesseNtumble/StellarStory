package stellarstory.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import stellarstory.api.transfer.ISideConfigurable;
import stellarstory.api.transfer.SideMode;
import stellarstory.api.transfer.TransferKind;
import stellarstory.common.blocks.AbstractEnergyBlock;

/**
 * Гаечный ключ механизмов.
 * <p>
 * ПКМ по блоку — поворот вокруг вертикальной оси.
 * Shift+ПКМ по грани — переключение роли стороны для энергии.
 * Роли сторон предметов будут в GUI (следующий этап), здесь только энергия.
 */
public class EnergyWrenchItem extends Item {

    public EnergyWrenchItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public net.minecraft.world.InteractionResult onItemUseFirst(@NotNull ItemStack stack,
                                                                @NotNull net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (player == null || context.getHand() != InteractionHand.MAIN_HAND) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Direction side = context.getClickedFace();

        // Shift+ПКМ — настройка роли стороны
        if (player.isSecondaryUseActive()) {
            if (!(level.getBlockEntity(pos) instanceof ISideConfigurable configurable)) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                SideMode mode = configurable.cycleSideMode(TransferKind.ENERGY, side);
                player.displayClientMessage(Component.translatable("gui.stellarstory.side_mode.set",
                        Component.translatable("gui.stellarstory.side." + side.getSerializedName()),
                        Component.translatable(mode.translationKey())), true);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }

        // ПКМ — поворот блока
        if (state.getBlock() instanceof AbstractEnergyBlock machine) {
            if (!level.isClientSide) {
                level.setBlock(pos, machine.cycleFacing(state), Block.UPDATE_ALL);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }

        return net.minecraft.world.InteractionResult.PASS;
    }
}
