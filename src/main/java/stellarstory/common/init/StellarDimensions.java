package stellarstory.common.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import stellarstory.StellarStory;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.common.worldgen.EmptyBiomeSource;
import stellarstory.common.worldgen.StationChunkGenerator;

import java.util.Optional;
import java.util.OptionalLong;

public class StellarDimensions {

    public static final DeferredRegister<DimensionType> DIMENSION_TYPES = DeferredRegister.create(Registries.DIMENSION_TYPE, StellarStory.MODID);
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, StellarStory.MODID);


    public static final RegistryObject<Codec<? extends ChunkGenerator>> STATION_CHUNK_GENERATOR =
            CHUNK_GENERATORS.register("station_chunk_generator", () -> StationChunkGenerator.CODEC);

    /*
    public static StarSystem solar_system = new StarSystem("solar_system");
    public static Star sol = new Star("sol").setParentStarSystem(solar_system);
    public static Planet overworld = new Planet("overworld").setParentStar(sol);
    public static Moon moon = (Moon) new Moon("moon").setParentPlanet(overworld).setReachable();
*/


    public static void initialization() {
        /*sol.getProperties().setIcon(ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, "textures/gui/celestialbodies/sol.png"));
        moon.getProperties().setPhysicalData(PhysicalData.Builder.create()
                .setDayLength(178000L)
                .setGravity(0.16F));
        moon.getProperties().setAtmosphereData(AtmosphereData.Builder.create()
                .setTemperature(-20F, 4.0F, -10F));
*/
  //      CelestialRegisters.registerStarSystem(StellarStory.prefix("solar_system"), solar_system);
    }
}
