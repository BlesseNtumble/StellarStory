package stellarstory.common.registers;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.RegistryObject;
import stellarstory.StellarRegistries;

import java.util.function.Supplier;

public class StellarBlocks {

    public static final RegistryObject<Block> test_block = registerBlock("test_block", () ->
            new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(6f)
                    .requiresCorrectToolForDrops()
            ));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        // register block to eventBus
        RegistryObject<T> tmp = StellarRegistries.BLOCKS.register(name, block);

        // register blockItem to eventBus
        StellarRegistries.ITEMS.register(name, () -> new BlockItem(tmp.get(), new Item.Properties().tab(StellarItems.STELLARSTORY_ITEMS)));

        return tmp;
    }
}
