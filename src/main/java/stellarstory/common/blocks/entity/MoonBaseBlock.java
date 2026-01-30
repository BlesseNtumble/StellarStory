package stellarstory.common.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import stellarstory.api.utils.block.state.property.StringProperty;
import stellarstory.common.init.StellarBlocks;
import stellarstory.common.init.StellarItems;

import java.util.List;

public class MoonBaseBlock extends Block {
    public static final StringProperty VARIANT = StringProperty.create("variant",
            List.of("grunt_1", "grunt_2", "grunt_3", "grunt_4",
                    "regolite_1", "regolite_2", "regolite_3", "regolite_4", "stone_1"));

    public MoonBaseBlock() {
        super(Properties.of().mapColor(MapColor.CLAY));
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, (String)VARIANT.getPossibleValues().toArray()[0]));
    }

    @Override
    protected void createBlockStateDefinition(@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }

    @Override
    public MutableComponent getName() {
        String variant = this.defaultBlockState().getValue(VARIANT);
        return Component.translatable("block.stellarstory.moon_basic_block_" + variant);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        String variant = state.getValue(VARIANT);
        for (RegistryObject<Item> item : StellarItems.MOON_BASIC_BLOCK_ITEMS) {
            if (item.getId().getPath().equals(StellarBlocks.MOON_BASIC_BLOCK.getId().getPath() + "_" + variant)) {
                return new ItemStack(item.get());
            }
        }
        return new ItemStack(StellarItems.MOON_BASIC_BLOCK_ITEMS.get(0).get());
    }

}
