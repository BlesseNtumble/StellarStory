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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CelestialBodyLoader {
    private static final Map<ResourceLocation, StarSystem> STAR_SYSTEMS = new HashMap<>();
    private static final Map<ResourceLocation, Star> STARS = new HashMap<>();
    private static final Map<ResourceLocation, Planet> PLANETS = new HashMap<>();
    private static final Map<ResourceLocation, Moon> MOONS = new HashMap<>();

    public static void loadCelestialBodies(ResourceManager resourceManager) {
        // Очистка предыдущих данных
        STAR_SYSTEMS.clear();
        STARS.clear();
        PLANETS.clear();
        MOONS.clear();

        loadStarSystems(resourceManager);
        loadCelestialBodies(resourceManager, "celestialbodies/stars", Star.CODEC, STARS, "Star");
        loadCelestialBodies(resourceManager, "celestialbodies/planets", Planet.CODEC, PLANETS, "Planet");
        loadCelestialBodies(resourceManager, "celestialbodies/moons", Moon.CODEC, MOONS, "Moon");

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

    private static void linkCelestialBodies() {

        for(Planet planets : PLANETS.values()) {
            planets.resolveChilds(MOONS);
        }

        for(Star stars : STARS.values()) {
            stars.resolvePlanets(PLANETS);
        }

        for (StarSystem system : STAR_SYSTEMS.values()) {
            system.resolveStars(STARS); // Разрешаем ссылки на звёзды
        }


    }

    private static void registerStarSystems() {
        STAR_SYSTEMS.forEach((id, system) -> {
            CelestialRegisters.registerStarSystem(id, system);
            StellarStory.getLogger().info("Registered Star System: " + system.getName());
        });
    }
}
