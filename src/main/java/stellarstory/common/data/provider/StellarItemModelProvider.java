package stellarstory.common.data.provider;

import net.minecraft.data.DataGenerator;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import stellarstory.StellarStory;

import java.util.function.Supplier;

public abstract class StellarItemModelProvider extends ItemModelProvider {
    public StellarItemModelProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, StellarStory.MODID, existingFileHelper);
    }

    private String blockName(Supplier<? extends Block> block) {
        return ForgeRegistries.BLOCKS.getKey(block.get()).getPath();
    }

    public ItemModelBuilder block(Supplier<? extends Block> block) {
        return block(block, blockName(block));
    }

    public ItemModelBuilder block(Supplier<? extends Block> block, String name) {
        return withExistingParent(blockName(block), modLoc("block/" + name));
    }

}
