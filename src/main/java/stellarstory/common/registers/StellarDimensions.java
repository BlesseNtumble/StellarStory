package stellarstory.common.registers;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.eventbus.api.IEventBus;
import stellarstory.StellarStory;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.Moon;
import stellarstory.api.celestial.Planet;
import stellarstory.api.celestial.Star;
import stellarstory.api.celestial.StarSystem;
import stellarstory.api.celestial.data.AtmosphereData;
import stellarstory.api.celestial.data.PhysicalData;
import stellarstory.common.celestials.solarsystems.world.MoonBiomeSource;

public class StellarDimensions {

    public static StarSystem solar_system = new StarSystem("solar_system");
    public static Star sol = new Star("sol").setParentStarSystem(solar_system);
    public static Planet overworld = new Planet("overworld").setParentStar(sol);
    public static Moon moon = (Moon) new Moon("moon").setParentPlanet(overworld).setReachable();

    public static ResourceKey<Biome> MOON_BIOME;
    public static void register(IEventBus e) {


        MOON_BIOME = ResourceKey.create(Registry.BIOME_REGISTRY, StellarStory.prefix("moon_default"));

        sol.getProperties().setIcon(new ResourceLocation(StellarStory.MODID, "textures/gui/celestialbodies/sol.png"));
        moon.getProperties().setPhysicalData(PhysicalData.Builder.create()
                .setDayLength(178000L)
                .setGravity(0.16F));
        moon.getProperties().setAtmosphereData(AtmosphereData.Builder.create()
                .setTemperature(-20F, 4.0F, -10F));

        CelestialRegisters.registerStarSystem(StellarStory.prefix("solar_system"), solar_system);

    }

    public static void commonRegister()
    {
        Registry.register(Registry.BIOME_SOURCE, StellarStory.prefix("moon_biomes"),
                MoonBiomeSource.CODEC);
    }
}
