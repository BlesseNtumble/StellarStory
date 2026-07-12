package stellarstory.data;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import stellarstory.StellarStory;
import stellarstory.common.blocks.MoonBaseBlock;
import stellarstory.common.init.StellarBlocks;

public class StellarBlockstatesData extends BlockStateProvider {
    public StellarBlockstatesData(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, StellarStory.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        Block moon_blocks = StellarBlocks.MOON_BASIC_BLOCK.get();
        VariantBlockStateBuilder builder = getVariantBuilder(moon_blocks);

        for(String variant : MoonBaseBlock.VARIANT.getPossibleValues()) {
            builder.partialState()
                    .with(MoonBaseBlock.VARIANT, variant)
                    .modelForState()
                    .modelFile(models().cubeAll(StellarBlocks.MOON_BASIC_BLOCK.getId().getPath() + "_" + variant, modLoc("block/moon/" + variant)))
                    .addModel();
        }
    }
}
