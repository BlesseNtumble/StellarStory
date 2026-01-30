package stellarstory.data;

import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import stellarstory.StellarStory;
import stellarstory.common.blocks.entity.MoonBaseBlock;
import stellarstory.common.init.StellarBlocks;

public class StellarItemModelData extends ItemModelProvider {
    public StellarItemModelData(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, StellarStory.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (String variant : MoonBaseBlock.VARIANT.getPossibleValues()) {
            String itemName = StellarBlocks.MOON_BASIC_BLOCK.getId().getPath() + "_" + variant;

            // Модель предмета
            withExistingParent(itemName, modLoc("block/" + itemName));
        }
    }
}
