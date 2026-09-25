package stellarstory.common.init;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import stellarstory.StellarStory;
import stellarstory.common.blocks.blockentity.CoalGeneratorBlockEntity;
import stellarstory.common.blocks.blockentity.ElectricFurnaceBlockEntity;
import stellarstory.common.blocks.blockentity.EnergyBatteryBlockEntity;
import stellarstory.common.blocks.blockentity.GrinderBlockEntity;

public class StellarBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> REGISTRY =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, StellarStory.MODID);

    public static final RegistryObject<BlockEntityType<EnergyBatteryBlockEntity>> ENERGY_BATTERY =
            REGISTRY.register("energy_battery", () -> BlockEntityType.Builder
                    .of(EnergyBatteryBlockEntity::new, StellarBlocks.ENERGY_BATTERY.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR =
            REGISTRY.register("coal_generator", () -> BlockEntityType.Builder
                    .of(CoalGeneratorBlockEntity::new, StellarBlocks.COAL_GENERATOR.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            REGISTRY.register("electric_furnace", () -> BlockEntityType.Builder
                    .of(ElectricFurnaceBlockEntity::new, StellarBlocks.ELECTRIC_FURNACE.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<GrinderBlockEntity>> GRINDER =
            REGISTRY.register("grinder", () -> BlockEntityType.Builder
                    .of(GrinderBlockEntity::new, StellarBlocks.GRINDER.get())
                    .build(null));
}
