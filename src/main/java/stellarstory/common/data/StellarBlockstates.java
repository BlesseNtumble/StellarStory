package stellarstory.common.data;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import stellarstory.common.data.provider.StellarBlockStateProvider;
import stellarstory.common.registers.StellarBlocks;

public class StellarBlockstates extends StellarBlockStateProvider {

    public StellarBlockstates(DataGenerator generator, ExistingFileHelper fileHelper) {
        super(generator, fileHelper);
    }

    @Override
    public String getName() {
        return "Stellar Story BlockStates";
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlock(StellarBlocks.test_block.get());
    }
}
