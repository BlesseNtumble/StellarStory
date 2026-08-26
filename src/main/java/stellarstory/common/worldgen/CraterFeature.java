package stellarstory.common.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import stellarstory.common.blocks.MoonBaseBlock;
import stellarstory.common.init.StellarBlocks;

import java.util.Optional;

/**
 * Generates impact craters using mathematical formulas.
 * Automatically selects rim/floor blocks based on the biome.
 *
 * Bowl shape:  depth(r) = D * (1 - (r/R)^2)           — parabolic bowl
 * Rim shape:   rim(r)  = H * (1 - ((r-R)/W)^2)       — parabolic rim
 * Central peak: peak(r) = D*0.15 * (1 - (r/(R*0.25))^2) — for R >= 12
 */
public class CraterFeature extends Feature<CraterFeatureConfiguration> {

    public CraterFeature(Codec<CraterFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CraterFeatureConfiguration> context) {
        WorldGenLevel level = (WorldGenLevel) context.level();
        CraterFeatureConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int radius = config.minRadius() + random.nextInt(Math.max(1, config.maxRadius() - config.minRadius() + 1));
        double depth = radius * config.depthRatio();
        int rimWidth = Math.max(2, radius / 3);
        int rimHeight = config.rimHeight();
        int cx = origin.getX();
        int cz = origin.getZ();
        int baseY = origin.getY();

        // Determine biome at crater center to select blocks
        BlockState rimBlock = resolveBlock(level, origin, true);
        BlockState floorBlock = resolveBlock(level, origin, false);

        int originChunkX = cx >> 4;
        int originChunkZ = cz >> 4;

        // Limit range to 15 (one full chunk) to prevent far-chunk writes
        int range = radius + rimWidth;
        if (range > 15) {
            range = 15;
            radius = Math.min(radius, range);
            depth = radius * config.depthRatio();
        }

        boolean generated = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (dist > range) continue;

                int x = cx + dx;
                int z = cz + dz;

                // Must be within the accessible 3x3 chunk area
                int targetChunkX = x >> 4;
                int targetChunkZ = z >> 4;
                if (Math.abs(targetChunkX - originChunkX) > 1 || Math.abs(targetChunkZ - originChunkZ) > 1) continue;

                // Find surface Y by scanning down from baseY+2
                int surfaceY = baseY + 2;
                while (surfaceY > level.getMinBuildHeight() && level.getBlockState(pos.set(x, surfaceY, z)).isAir()) {
                    surfaceY--;
                }

                if (dist <= radius) {
                    // === Crater bowl (parabolic) ===
                    double t = dist / radius;
                    double bowlDepth = depth * (1.0 - t * t);
                    int floorY = surfaceY - (int) Math.round(bowlDepth);

                    for (int y = surfaceY; y > floorY; y--) {
                        level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                    level.setBlock(pos.set(x, floorY, z), floorBlock, 2);

                    // Central peak for large craters
                    if (radius >= 12 && dist < radius * 0.25) {
                        double pt = dist / (radius * 0.25);
                        double peakHeight = depth * 0.15 * (1.0 - pt * pt);
                        int peakTop = floorY + (int) Math.round(peakHeight);
                        for (int y = floorY + 1; y <= peakTop; y++) {
                            level.setBlock(pos.set(x, y, z), floorBlock, 2);
                        }
                    }

                    generated = true;
                } else {
                    // === Raised rim (parabolic) ===
                    double rt = (dist - radius) / rimWidth;
                    double rimH = rimHeight * (1.0 - rt * rt);
                    int rimTop = surfaceY + (int) Math.round(rimH);

                    for (int y = surfaceY + 1; y <= rimTop; y++) {
                        if (level.getBlockState(pos.set(x, y, z)).isAir()) {
                            level.setBlock(pos.set(x, y, z), rimBlock, 2);
                        }
                    }
                    generated = true;
                }
            }
        }

        return generated;
    }

    /**
     * Resolves rim (isRim=true) or floor (isRim=false) block based on biome.
     * Falls back to config defaults if biome is not recognized.
     */
    private BlockState resolveBlock(WorldGenLevel level, BlockPos pos, boolean isRim) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        Optional<ResourceKey<Biome>> keyOpt = biomeHolder.unwrapKey();

        if (keyOpt.isPresent()) {
            String biomeName = keyOpt.get().location().getPath();

            if (biomeName.contains("maria")) {
                // Lunar maria — dark basaltic plains
                return isRim ? moonBlock("regolite_3") : moonBlock("grunt_3");
            } else if (biomeName.contains("highlands")) {
                // Lunar highlands — bright, heavily cratered
                return isRim ? moonBlock("regolite_1") : moonBlock("stone_1");
            } else if (biomeName.contains("polar")) {
                // Polar regions — fine regolith
                return isRim ? moonBlock("regolite_4") : moonBlock("grunt_4");
            } else if (biomeName.contains("moon_biome_1")) {
                return isRim ? moonBlock("regolite_1") : moonBlock("stone_1");
            } else if (biomeName.contains("moon_biome_2")) {
                return isRim ? moonBlock("regolite_2") : moonBlock("grunt_2");
            }
        }

        // Default fallback
        return isRim ? moonBlock("regolite_1") : moonBlock("stone_1");
    }

    private BlockState moonBlock(String variant) {
        var moonBlock = StellarBlocks.MOON_BASIC_BLOCK.get();
        return moonBlock.defaultBlockState().setValue(MoonBaseBlock.VARIANT, variant);
    }
}

