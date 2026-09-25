package stellarstory.common.init;

import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import stellarstory.StellarStory;
import stellarstory.common.blocks.MoonBaseBlock;
import stellarstory.common.item.EnergyWrenchItem;

import java.util.ArrayList;
import java.util.List;

public class StellarItems {
    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, StellarStory.MODID);

    public static final List<RegistryObject<Item>> MOON_BASIC_BLOCK_ITEMS = registerMoonBasicBlockItems();

    // Энергетические механизмы
    public static final RegistryObject<Item> ENERGY_BATTERY = block(StellarBlocks.ENERGY_BATTERY);
    public static final RegistryObject<Item> COAL_GENERATOR = block(StellarBlocks.COAL_GENERATOR);
    public static final RegistryObject<Item> ELECTRIC_FURNACE = block(StellarBlocks.ELECTRIC_FURNACE);
    public static final RegistryObject<Item> GRINDER = block(StellarBlocks.GRINDER);
    public static final RegistryObject<Item> ENERGY_PIPE = block(StellarBlocks.ENERGY_PIPE);
    public static final RegistryObject<Item> ENERGY_WRENCH = REGISTRY.register("energy_wrench", EnergyWrenchItem::new);

    private static RegistryObject<Item> block(RegistryObject<Block> block) {
        return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<Item> doubleBlock(RegistryObject<Block> block) {
        return REGISTRY.register(block.getId().getPath(), () -> new DoubleHighBlockItem(block.get(), new Item.Properties()));
    }

    static {

    }

    private static List<RegistryObject<Item>> registerMoonBasicBlockItems() {
        List<RegistryObject<Item>> items = new ArrayList<>();
        for (String variant : MoonBaseBlock.VARIANT.getPossibleValues()) {
            String itemName = StellarBlocks.MOON_BASIC_BLOCK.getId().getPath() + "_" + variant;
            RegistryObject<Item> item = REGISTRY.register(itemName, () ->
                    new BlockItem(StellarBlocks.MOON_BASIC_BLOCK.get(), new Item.Properties()) {
                        @Override
                        protected BlockState getPlacementState(BlockPlaceContext context) {
                            return this.getBlock().defaultBlockState().setValue(MoonBaseBlock.VARIANT, variant);
                        }

                        @Override
                        public String getDescriptionId() {
                            // Возвращаем уникальный ключ перевода
                            return "item.stellarstory." + itemName;
                        }
                    });
            items.add(item);
        }
        return items;
    }
}
