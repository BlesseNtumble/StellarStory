package stellarstory.common.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import stellarstory.StellarStory;

/**
 * Registers custom feature types for world generation.
 */
public class StellarFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, StellarStory.MODID);

    public static final RegistryObject<Feature<CraterFeatureConfiguration>> CRATER =
            FEATURES.register("crater", () -> new CraterFeature(CraterFeatureConfiguration.CODEC));
}
