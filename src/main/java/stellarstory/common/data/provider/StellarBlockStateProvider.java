package stellarstory.common.data.provider;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import stellarstory.StellarStory;

public abstract class StellarBlockStateProvider extends BlockStateProvider {

    public StellarBlockStateProvider(DataGenerator generator, ExistingFileHelper fileHelper) {
        super(generator, StellarStory.MODID, fileHelper);
    }
}
