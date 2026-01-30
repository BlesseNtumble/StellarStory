package stellarstory.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import stellarstory.api.celestial.*;
import stellarstory.api.celestial.StarSystem;
import stellarstory.api.celestial.basic_type.Planet;
import stellarstory.api.celestial.basic_type.Star;

import java.util.*;
import java.util.stream.Collectors;

public final class CelestialRegisters {

    private static Map<ResourceLocation, StarSystem> STAR_SYSTEMS = new HashMap<>();
    private static CelestialBody EMPTY = Star.builder().build();

    public static void registerStarSystem(ResourceLocation rl, StarSystem star_system){
        STAR_SYSTEMS.put(rl, star_system);
    }

    public static CelestialBody getCelestialBodyFromPlayer(Player player) {
        return getCelestialBody(player.level().dimension().location().getPath());
    }

    public static CelestialBody getCelestialBodyFromLevel(Level level)
    {
        return getCelestialBody(level.dimension().location().getPath());
    }

    public static CelestialBody getCelestialBody(String name) {
        for (Map.Entry<ResourceLocation, StarSystem> systemEntry : STAR_SYSTEMS.entrySet()) {
            StarSystem system = systemEntry.getValue();

            // Поиск среди звезд
            Optional<Star> starMatch = system.getStars().stream()
                    .filter(star -> name.equals(star.getName()))
                    .findFirst();
            if (starMatch.isPresent()) {
                return starMatch.get();
            }

            // Поиск среди планет
            Optional<Planet> planetMatch = system.getStars().stream()
                    .flatMap(star -> star.getPlanets().stream())
                    .filter(planet -> name.equals(planet.getName()))
                    .findFirst();
            if (planetMatch.isPresent()) {
                return planetMatch.get();
            }

            // Поиск среди дочерних тел
            Optional<IChildBody> childMatch = system.getStars().stream()
                    .flatMap(star -> star.getPlanets().stream())
                    .flatMap(planet -> planet.getChilds().stream())
                    .filter(child -> name.equals(((CelestialBody)child).getName()))
                    .findFirst();
            if (childMatch.isPresent()) {
                return (CelestialBody) childMatch.get();
            }
        }

        // Если ничего не найдено
        return EMPTY;
    }

    public static Set<Planet> getAllPlanets() {
        return STAR_SYSTEMS.values().stream()
                .flatMap(system -> system.getStars().stream())
                .flatMap(star -> star.getPlanets().stream())
                .collect(Collectors.toSet());
    }

    public static Set<IChildBody> getAllChildBodies() {
        return STAR_SYSTEMS.values().stream()
                .flatMap(system -> system.getStars().stream()) // Плоский поток всех звезд
                .flatMap(star -> star.getPlanets().stream())   // Плоский поток всех планет
                .flatMap(planet -> planet.getChilds().stream()) // Плоский поток всех дочерних тел
                .collect(Collectors.toSet());                  // Сбор в множество
    }

    public static StarSystem getStarSystem(ResourceLocation rl) {
        return ImmutableMap.copyOf(STAR_SYSTEMS).get(rl);
    }

    public static ImmutableMap<ResourceLocation, StarSystem> getStarSystems() {
        return ImmutableMap.copyOf(STAR_SYSTEMS);
    }

    public static ImmutableList<Star> getStarsFromSystem(StarSystem system) {
        return ImmutableList.copyOf(system.getStars());
    }

    public static StarSystem getStarSystemFromStar(Star star){
        for(StarSystem starSystem : getStarSystems().values())
            if(starSystem.getStars().contains(star))
                return starSystem;

        return null;
    }

    public static ImmutableSet<Planet> getPlanetsFromStar(Star starFind) {
        return ImmutableSet.copyOf(STAR_SYSTEMS.values().stream()
                .flatMap(system -> system.getStars().stream())
                .flatMap(star -> star.getPlanets().stream())
                .filter(star -> star.equals(starFind))
                .collect(ImmutableSet.toImmutableSet()));
    }

    public static ResourceLocation prefix(String modid, String name) {
        return ResourceLocation.fromNamespaceAndPath(modid, name.toLowerCase(Locale.ROOT));
    }
}
