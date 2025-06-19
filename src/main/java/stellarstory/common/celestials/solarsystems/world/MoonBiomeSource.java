package stellarstory.common.celestials.solarsystems.world;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import stellarstory.common.registers.StellarDimensions;
import stellarstory.mixin.MixinBiome;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class MoonBiomeSource extends BiomeSource {
    private final List<Holder<Biome>> biomes; // Список биомов
    private final Registry<Biome> biomeRegistry; // Нужен для доступа к реестру
    private final PerlinNoise noise; // Шум для плавных переходов (опционально)

    protected MoonBiomeSource(Registry<Biome> registry, List<Holder<Biome>> biomes) {
        super(biomes);
        this.biomeRegistry = registry;
        this.biomes = biomes;

        int octaves = 4;
        double[] coefficients = {1.0, 0.5, 0.25, 0.125}; // 4 октавы
        this.noise = PerlinNoise.create(RandomSource.create(42L), octaves, DoubleList.of(coefficients));

    }

    public Registry<Biome> getBiomeRegistry() {
        return biomeRegistry;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        Random random = new Random((long) x * 3128871L + (long) z);
        double totalWeight = biomes.stream().mapToDouble(MoonChunkGenerator.BiomeWeight::weight).sum();
        return null;
    }

    // Метод для случайного выбора биома из списка
    private Holder<Biome> getRandomBiome(int x, int z) {
        Random random = new Random((long) x * 3128871L + (long) z); // Сид из координат
        int index = random.nextInt(biomes.size());
        return biomes.get(index);
    }

    // Пример выбора биома по шуму (опционально)
    private Holder<Biome> getBiomeByNoise(int x, int y, int z) {
        double noiseValue = noise.getValue(x / 100.0, y / 100.0, z / 100.0);
        // Пример: выбираем биом по значению шума
        return biomes.get((int) (noiseValue * biomes.size()) % biomes.size());
    }
}
