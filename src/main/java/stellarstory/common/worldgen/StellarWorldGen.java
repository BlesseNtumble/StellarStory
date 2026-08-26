package stellarstory.common.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import stellarstory.StellarStory;
import stellarstory.common.blocks.MoonBaseBlock;
import stellarstory.common.init.StellarBlocks;

import java.util.List;

public class StellarWorldGen {

    public static final ResourceKey<ConfiguredFeature<?, ?>> CRATER_SMALL = cfKey("crater_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CRATER_LARGE = cfKey("crater_large");

    public static final ResourceKey<PlacedFeature> CRATER_SMALL_PLACED = pfKey("crater_small");
    public static final ResourceKey<PlacedFeature> CRATER_LARGE_PLACED = pfKey("crater_large");

    private static ResourceKey<ConfiguredFeature<?, ?>> cfKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, StellarStory.prefix(name));
    }

    private static ResourceKey<PlacedFeature> pfKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, StellarStory.prefix(name));
    }

    public static void bootstrapConfigured(BootstapContext<ConfiguredFeature<?, ?>> ctx) {
        register(ctx, CRATER_SMALL, StellarFeatures.CRATER.get(), new CraterFeatureConfiguration(
                3, 6, 0.5f, 0,
                moonBlock("regolite_1"),
                moonBlock("regolite_1")
        ));
        register(ctx, CRATER_LARGE, StellarFeatures.CRATER.get(), new CraterFeatureConfiguration(
                8, 12, 0.3f, 1,
                moonBlock("regolite_1"),
                moonBlock("regolite_1")
        ));
    }

    private static BlockState moonBlock(String variant) {
        return StellarBlocks.MOON_BASIC_BLOCK.get().defaultBlockState().setValue(MoonBaseBlock.VARIANT, variant);
    }

    public static void bootstrapPlaced(BootstapContext<PlacedFeature> ctx) {
        HolderGetter<ConfiguredFeature<?, ?>> cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        register(ctx, CRATER_SMALL_PLACED, cf.getOrThrow(CRATER_SMALL),
                RarityFilter.onAverageOnceEvery(6),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome());
        register(ctx, CRATER_LARGE_PLACED, cf.getOrThrow(CRATER_LARGE),
                RarityFilter.onAverageOnceEvery(20),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome());
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(
            BootstapContext<ConfiguredFeature<?, ?>> ctx,
            ResourceKey<ConfiguredFeature<?, ?>> key,
            F feature, FC config) {
        ctx.register(key, new ConfiguredFeature<>(feature, config));
    }

    private static void register(
            BootstapContext<PlacedFeature> ctx,
            ResourceKey<PlacedFeature> key,
            Holder<ConfiguredFeature<?, ?>> feature,
            PlacementModifier... modifiers) {
        ctx.register(key, new PlacedFeature(feature, List.of(modifiers)));
    }
}
