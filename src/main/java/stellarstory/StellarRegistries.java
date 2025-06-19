package stellarstory;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import stellarstory.api.celestial.Planet;
import stellarstory.api.celestial.Star;
import stellarstory.api.celestial.StarSystem;

public class StellarRegistries {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, StellarStory.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, StellarStory.MODID);

    public static final DeferredRegister<DimensionType> DIMENSION_TYPES = DeferredRegister.create(Registry.DIMENSION_TYPE_REGISTRY, StellarStory.MODID);
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister.create(Registry.CHUNK_GENERATOR_REGISTRY, StellarStory.MODID);

    public static final ResourceKey<Registry<StarSystem>> STARSYSTEMS_KEY = ResourceKey.createRegistryKey(
            new ResourceLocation(StellarStory.MODID, "data/celestial_bodies/systems")
    );
    public static final ResourceKey<Registry<Planet>> PLANETS_KEY = ResourceKey.createRegistryKey(
            new ResourceLocation(StellarStory.MODID, "data/celestial_bodies/planets")
    );


}
