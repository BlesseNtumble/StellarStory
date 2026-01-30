package stellarstory.common.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.registries.DeferredRegister;
import stellarstory.StellarStory;
import stellarstory.api.celestial.CelestialBody;

public class StellarDimensions {
    public static final DeferredRegister<CelestialBody> CELESTIAL_BODIES = DeferredRegister.create(ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, "celestialbodies"), StellarStory.MODID);

    public static final DeferredRegister<DimensionType> DIMENSION_TYPES = DeferredRegister.create(Registries.DIMENSION_TYPE, StellarStory.MODID);
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, StellarStory.MODID);


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
