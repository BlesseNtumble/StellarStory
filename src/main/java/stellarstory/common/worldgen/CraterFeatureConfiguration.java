package stellarstory.common.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration for mathematical impact crater generation.
 * Crater shape: parabolic bowl with raised rim and optional central peak.
 */
public record CraterFeatureConfiguration(
        int minRadius,
        int maxRadius,
        float depthRatio,
        int rimHeight,
        BlockState rimBlock,
        BlockState floorBlock
) implements FeatureConfiguration {

    public static final Codec<CraterFeatureConfiguration> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("min_radius").forGetter(CraterFeatureConfiguration::minRadius),
                    Codec.INT.fieldOf("max_radius").forGetter(CraterFeatureConfiguration::maxRadius),
                    Codec.FLOAT.fieldOf("depth_ratio").orElse(0.4f).forGetter(CraterFeatureConfiguration::depthRatio),
                    Codec.INT.fieldOf("rim_height").orElse(2).forGetter(CraterFeatureConfiguration::rimHeight),
                    BlockState.CODEC.fieldOf("rim_block").forGetter(CraterFeatureConfiguration::rimBlock),
                    BlockState.CODEC.fieldOf("floor_block").forGetter(CraterFeatureConfiguration::floorBlock)
            ).apply(instance, CraterFeatureConfiguration::new)
    );
}
