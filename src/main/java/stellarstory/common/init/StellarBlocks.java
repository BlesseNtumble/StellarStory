package stellarstory.common.init;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import stellarstory.StellarStory;
import stellarstory.common.blocks.MoonBaseBlock;

public class StellarBlocks {
    public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, StellarStory.MODID);
    public static final RegistryObject<Block> MOON_BASIC_BLOCK = REGISTRY.register("moon_basic_block", MoonBaseBlock::new);


}
