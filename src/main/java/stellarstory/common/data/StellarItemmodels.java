package stellarstory.common.data;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import stellarstory.common.data.provider.StellarItemModelProvider;
import stellarstory.common.registers.StellarBlocks;

public class StellarItemmodels extends StellarItemModelProvider {

    public StellarItemmodels(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Stellar Story ItemModels";
    }

    @Override
    protected void registerModels() {
        block(StellarBlocks.test_block);
    }
}
