package stellarstory.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import stellarstory.StellarStory;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.IChildBody;
import stellarstory.api.celestial.StarSystem;
import stellarstory.api.celestial.basic_type.Moon;
import stellarstory.api.celestial.basic_type.Planet;
import stellarstory.api.celestial.basic_type.Star;
import stellarstory.api.celestial.basic_type.Asteroid;
import stellarstory.api.celestial.basic_type.Comet;
import stellarstory.api.celestial.Barycenter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CelestialBodyLoader {
    private static final Map<ResourceLocation, StarSystem> STAR_SYSTEMS = new HashMap<>();
    private static final Map<ResourceLocation, Star> STARS = new HashMap<>();
    private static final Map<ResourceLocation, Planet> PLANETS = new HashMap<>();
    private static final Map<ResourceLocation, Moon> MOONS = new HashMap<>();
    private static final Map<ResourceLocation, Asteroid> ASTEROIDS = new HashMap<>();
    private static final Map<ResourceLocation, Comet> COMETS = new HashMap<>();
    private static final Map<ResourceLocation, Barycenter> BARYCENTERS = new HashMap<>();

    public static void loadCelestialBodies(ResourceManager resourceManager) {
        // Очистка предыдущих данных
        STAR_SYSTEMS.clear();
        STARS.clear();
        PLANETS.clear();
        MOONS.clear();
        ASTEROIDS.clear();
        COMETS.clear();
        BARYCENTERS.clear();

        loadStarSystems(resourceManager);
        loadCelestialBodies(resourceManager, "celestialbodies/stars", Star.CODEC, STARS, "Star");
        loadCelestialBodies(resourceManager, "celestialbodies/planets", Planet.CODEC, PLANETS, "Planet");
        loadCelestialBodies(resourceManager, "celestialbodies/moons", Moon.CODEC, MOONS, "Moon");
        loadCelestialBodies(resourceManager, "celestialbodies/asteroids", Asteroid.CODEC, ASTEROIDS, "Asteroid");
        loadCelestialBodies(resourceManager, "celestialbodies/comets", Comet.CODEC, COMETS, "Comet");
        loadBarycenters(resourceManager);

        linkCelestialBodies();
        registerStarSystems();
    }

    private static void loadStarSystems(ResourceManager resourceManager) {
        resourceManager.listResources("celestialbodies/systems", path -> path.toString().endsWith(".json")).forEach((path, resource) -> {
            try (BufferedReader reader = resource.openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);

                // Десериализация с помощью Codec
                DataResult<StarSystem> result = StarSystem.CODEC.parse(JsonOps.INSTANCE, json);
                result.result().ifPresent(starSystem -> {
                    StellarStory.getLogger().info("test");
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, starSystem.getName().toLowerCase());
                    STAR_SYSTEMS.put(id, starSystem);
                    StellarStory.getLogger().info("Loaded Star System: " + starSystem.getName());
                });

                result.error().ifPresent(error -> {
                    StellarStory.getLogger().error("Failed to load Star System from file: " + path + " - " + error.message());
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static <T extends CelestialBody> void loadCelestialBodies(
            ResourceManager resourceManager,
            String folderPath,
            Codec<T> codec,
            Map<ResourceLocation, T> map,
            String type) {
            resourceManager.listResources(folderPath, path -> path.toString().endsWith(".json")).forEach((path, resource) -> {
            try (BufferedReader reader = resource.openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);

                // Десериализация с помощью Codec
                DataResult<T> result = codec.parse(JsonOps.INSTANCE, json);
                result.result().ifPresent(celestialBody -> {
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, celestialBody.getName().toLowerCase());
                    map.put(id, celestialBody);
                    StellarStory.getLogger().info("Loaded " + type + ": " + celestialBody.getName());
                });

                result.error().ifPresent(error -> {
                    StellarStory.getLogger().error("Failed to load " + type + " from file: " + path + " - " + error.message());
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void loadBarycenters(ResourceManager resourceManager) {
        resourceManager.listResources("celestialbodies/barycenters", path -> path.toString().endsWith(".json")).forEach((path, resource) -> {
            try (BufferedReader reader = resource.openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                DataResult<Barycenter> result = Barycenter.CODEC.parse(JsonOps.INSTANCE, json);
                result.result().ifPresent(barycenter -> {
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, barycenter.getName().toLowerCase());
                    BARYCENTERS.put(id, barycenter);
                    StellarStory.getLogger().info("Loaded Barycenter: " + barycenter.getName());
                });
                result.error().ifPresent(error -> {
                    StellarStory.getLogger().error("Failed to load Barycenter from file: " + path + " - " + error.message());
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void linkCelestialBodies() {
        StellarStory.getLogger().info("=== LINKING CELESTIAL BODIES ===");
        StellarStory.getLogger().info("PLANETS map size: " + PLANETS.size());
        for (Map.Entry<ResourceLocation, Planet> entry : PLANETS.entrySet()) {
            StellarStory.getLogger().info("  PLANET: " + entry.getKey() + " -> " + entry.getValue().getName());
        }

        for(Planet planets : PLANETS.values()) {
            planets.resolveChilds(MOONS);
        }

        for (Star stars : STARS.values()) {
            StellarStory.getLogger().info("Linking star: " + stars.getName() + ", planets refs: " + stars.getPlanetReferences());
            stars.resolvePlanets(PLANETS);
            StellarStory.getLogger().info("Star " + stars.getName() + " now has " + stars.getPlanets().size() + " planets: " + stars.getPlanets());
            stars.resolveAsteroids(ASTEROIDS);
            stars.resolveComets(COMETS);
            stars.resolveBarycenter(BARYCENTERS);
        }

        for (StarSystem system : STAR_SYSTEMS.values()) {
            system.resolveStars(STARS);
            system.resolveBarycenters(BARYCENTERS);
        }


    }

    private static void registerStarSystems() {
        STAR_SYSTEMS.forEach((id, system) -> {
            CelestialRegisters.registerStarSystem(id, system);
            StellarStory.getLogger().info("Registered Star System: " + system.getName());
        });
    }
}
