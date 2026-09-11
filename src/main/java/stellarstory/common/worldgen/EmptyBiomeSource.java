package stellarstory.common.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.stream.Stream;

public class EmptyBiomeSource extends BiomeSource {

    // Кодок для сохранения (нужен, но в космосе не используется)
    public static final Codec<EmptyBiomeSource> CODEC = Codec.unit(EmptyBiomeSource::new);
    public EmptyBiomeSource() {
        super();
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.empty();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int p_204238_, int p_204239_, int p_204240_, Climate.Sampler p_204241_) {
        return ForgeRegistries.BIOMES.getHolder(Biomes.THE_VOID).get();
    }


}