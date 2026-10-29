package stellarstory.common.init;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import stellarstory.StellarStory;
import stellarstory.common.blocks.CoalGeneratorBlock;
import stellarstory.common.blocks.ElectricFurnaceBlock;
import stellarstory.common.blocks.FluidInfuserBlock;
import stellarstory.common.blocks.FluidStorageBlock;
import stellarstory.common.blocks.GrinderBlock;
import stellarstory.common.blocks.MoonBaseBlock;
import stellarstory.common.blocks.EnergyBatteryBlock;
import stellarstory.common.blocks.pipe.EnergyPipeBlock;
import stellarstory.common.blocks.pipe.FluidPipeBlock;

public class StellarBlocks {
    public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, StellarStory.MODID);
    public static final RegistryObject<Block> MOON_BASIC_BLOCK = REGISTRY.register("moon_basic_block", MoonBaseBlock::new);

    // Энергетические механизмы
    public static final RegistryObject<Block> ENERGY_BATTERY = REGISTRY.register("energy_battery", EnergyBatteryBlock::new);
    public static final RegistryObject<Block> COAL_GENERATOR = REGISTRY.register("coal_generator", CoalGeneratorBlock::new);
    public static final RegistryObject<Block> ELECTRIC_FURNACE = REGISTRY.register("electric_furnace", ElectricFurnaceBlock::new);
    public static final RegistryObject<Block> GRINDER = REGISTRY.register("grinder", GrinderBlock::new);
    public static final RegistryObject<Block> FLUID_STORAGE = REGISTRY.register("fluid_storage", FluidStorageBlock::new);
    public static final RegistryObject<Block> FLUID_INFUSER = REGISTRY.register("fluid_infuser", FluidInfuserBlock::new);

    // Трубы
    public static final RegistryObject<Block> ENERGY_PIPE = REGISTRY.register("energy_pipe", EnergyPipeBlock::new);
    public static final RegistryObject<Block> FLUID_PIPE = REGISTRY.register("fluid_pipe", FluidPipeBlock::new);


}
