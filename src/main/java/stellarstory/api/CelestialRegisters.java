package stellarstory.api;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.registries.DeferredRegister;
import stellarstory.StellarStory;
import stellarstory.api.celestial.*;

import java.util.*;

public final class CelestialRegisters {

    private static Map<ResourceLocation, StarSystem> STAR_SYSTEMS = new HashMap<>();

    public static void registerStarSystem(ResourceLocation rl, StarSystem star_system){
        STAR_SYSTEMS.put(rl, star_system);

    }

    public static CelestialBody getCelestialBodyFromPlayer(Player player) {
        return getCelestialBody(player.level.dimension().location().getPath());
    }

    public static CelestialBody getCelestialBodyFromLevel(Level level)
    {
        return getCelestialBody(level.dimension().location().getPath());
    }

    public static CelestialBody getCelestialBody(String name) {
        for(Map.Entry<ResourceLocation, StarSystem> system_map : STAR_SYSTEMS.entrySet()){
            StarSystem system = system_map.getValue();
            ImmutableSet<Star> stars = system.getStars();
            ImmutableSet<Planet> planets = system.getPlanets();
            ImmutableSet<IChildBody> childs = system.getChilds();

            CelestialBody body = stars.stream().filter(star -> name.equals(star.getName())).findAny().orElse(null);

            if(body == null)
                body = planets.stream().filter(planet -> name.equals(planet.getName())).findAny().orElse(null);
            if(body == null)
                body = (CelestialBody) childs.stream().filter(child -> name.equals(((CelestialBody)child).getName())).findAny().orElse(null);

            return body == null ? CelestialBody.EMPTY : body;
        }
        return CelestialBody.EMPTY;
    }

    public static Set<Planet> getAllPlanets() {
        Set<Planet> allPlanets = new HashSet<Planet>();
        for (Map.Entry<ResourceLocation, StarSystem> system_map : STAR_SYSTEMS.entrySet()) {
            StarSystem system = system_map.getValue();
            ImmutableSet<Planet> planets = system.getPlanets();
            allPlanets.addAll(planets);
        }
        return allPlanets;
    }

    public static Set<IChildBody> getAllChildBodies() {
        Set<IChildBody> allChilds = new HashSet<IChildBody>();
        for (Map.Entry<ResourceLocation, StarSystem> system_map : STAR_SYSTEMS.entrySet()) {
            StarSystem system = system_map.getValue();
            ImmutableSet<IChildBody> childs = system.getChilds();
            allChilds.addAll(childs);
        }
        return allChilds;
    }

    public static StarSystem getStarSystem(ResourceLocation rl) {
        return ImmutableMap.copyOf(STAR_SYSTEMS).get(rl);
    }

    public static ImmutableMap<ResourceLocation, StarSystem> getStarSystems() {
        return ImmutableMap.copyOf(STAR_SYSTEMS);
    }

    public static ImmutableSet<Star> getStarsFromSystem(StarSystem system) {
        return system.getStars();
    }

    public static StarSystem getStarSystemFromStar(Star star){
        for(StarSystem starSystem : getStarSystems().values())
            if(starSystem.getStars().contains(star))
                return starSystem;

        return null;
    }
    public static ImmutableSet<Planet> getPlanetsFromStar(Star star) {
        Set<Planet> planets = new HashSet<>();
       /* for(Planet planet : star.getParentSystem().getPlanets()) {
            if(planet.getParentStar().equals(star)) planets.add(planet);
        }*/
        return ImmutableSet.copyOf(planets);
    }

    public static ImmutableSet<IChildBody> getChildsFromPlanet(Planet planet) {
        Set<IChildBody> childs = new HashSet<>();
   /*     for(IChildBody child : planet.getParentStar().getParentSystem().getChilds()) {
            if(child.getParentPlanet().equals(planet)) childs.add(child);
        }*/
        return ImmutableSet.copyOf(childs);
    }
    public static ResourceLocation prefix(String modid, String name) {
        return new ResourceLocation(modid, name.toLowerCase(Locale.ROOT));
    }
}
