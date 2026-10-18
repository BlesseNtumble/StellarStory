package stellarstory.common.init;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import stellarstory.StellarStory;
import stellarstory.common.blocks.CoalGeneratorBlock;
import stellarstory.common.blocks.ElectricFurnaceBlock;
import stellarstory.common.blocks.GrinderBlock;
import stellarstory.common.blocks.MoonBaseBlock;
import stellarstory.common.blocks.EnergyBatteryBlock;
import stellarstory.common.blocks.pipe.EnergyPipeBlock;

public class StellarBlocks {
    public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, StellarStory.MODID);
    public static final RegistryObject<Block> MOON_BASIC_BLOCK = REGISTRY.register("moon_basic_block", MoonBaseBlock::new);

    // Энергетические механизмы
    public static final RegistryObject<Block> ENERGY_BATTERY = REGISTRY.register("energy_battery", EnergyBatteryBlock::new);
    public static final RegistryObject<Block> COAL_GENERATOR = REGISTRY.register("coal_generator", CoalGeneratorBlock::new);
    public static final RegistryObject<Block> ELECTRIC_FURNACE = REGISTRY.register("electric_furnace", ElectricFurnaceBlock::new);
    public static final RegistryObject<Block> GRINDER = REGISTRY.register("grinder", GrinderBlock::new);

    // Трубы
    public static final RegistryObject<Block> ENERGY_PIPE = REGISTRY.register("energy_pipe", EnergyPipeBlock::new);


}
