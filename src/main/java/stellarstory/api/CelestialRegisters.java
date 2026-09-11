package stellarstory.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.network.PacketDistributor;
import stellarstory.StellarStory;
import stellarstory.api.celestial.*;
import stellarstory.api.celestial.StarSystem;
import stellarstory.api.celestial.basic_type.*;
import stellarstory.common.capability.PlayerCapability;
import stellarstory.common.init.StellarCapabilities;
import stellarstory.common.init.StellarDimensions;
import stellarstory.common.level.StationBuildData;
import stellarstory.common.worldgen.StationChunkGenerator;
import stellarstory.network.NetworkHandler;
import stellarstory.network.packet.client.SyncStationUnlockPacket;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CelestialRegisters {


    private static final Map<ResourceLocation, StarSystem> STAR_SYSTEMS = Maps.newHashMap();
    private static final Map<ResourceLocation, Station> STATIONS = Maps.newHashMap();
    private static CelestialBody EMPTY = Star.builder().build();

    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);

    public static void reset() {
        STAR_SYSTEMS.clear();
        STATIONS.clear();
    }

    public static void registerStarSystem(ResourceLocation rl, StarSystem star_system){
        STAR_SYSTEMS.put(rl, star_system);
    }

    public static void registerStation(ResourceLocation id, Station station) {
        STATIONS.put(id, station);
    }

    public static CelestialBody getCelestialBodyFromPlayer(Player player) {
        return getCelestialBody(player.level().dimension().location().getPath());
    }

    public static CelestialBody getCelestialBodyFromLevel(Level level)    {
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

            // Поиск среди астероидов (звёзд и планет)
            Optional<Asteroid> asteroidMatch = system.getStars().stream()
                    .flatMap(star -> Stream.concat(
                            star.getAsteroids().stream(),
                            star.getPlanets().stream().flatMap(planet -> planet.getAsteroids().stream())))
                    .filter(asteroid -> name.equals(asteroid.getName()))
                    .findFirst();
            if (asteroidMatch.isPresent()) {
                return asteroidMatch.get();
            }

            // Поиск среди комет
            Optional<Comet> cometMatch = system.getStars().stream()
                    .flatMap(star -> star.getComets().stream())
                    .filter(comet -> name.equals(comet.getName()))
                    .findFirst();
            if (cometMatch.isPresent()) {
                return cometMatch.get();
            }
        }

        // Если ничего не найдено
        return null;
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


    public static ResourceLocation getId(CelestialBody body) {
        return ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, body.getName().toLowerCase(Locale.ROOT));
    }

    public static void unlock(ServerPlayer player, Station station) {
        station.unlockForPlayer(player.getUUID());
        ServerLevel level = player.server.getLevel(station.getDimensionKey());
        if (level != null) buildOnce(level, station);

        // Синхронизируем с клиентом
        NetworkHandler.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(()->player), new SyncStationUnlockPacket(player.getUUID(), station.getName(), true));

        // Обновляем карту
        // (можно отправить пакет клиенту, чтобы перерисовать карту)
    }

    /** Проверка условий (пример) */
    public static boolean canUnlock(ServerPlayer player, Station station) {
        PlayerCapability cap = player.getCapability(StellarCapabilities.PLAYER_CAPABILITY).orElse(null);
        if (cap == null) return false;

        return true;
    }

    public static boolean teleportToStation(ServerPlayer player, Station station) {
        ServerLevel level = player.server.getLevel(station.getDimensionKey());
        if (level == null) { // измерения нет в датапаке
            StellarStory.getLogger().error("Station {} has no dimension {}", station.getName(), station.getDimensionId());
            return false;
        }
        player.changeDimension(level);
        player.setPos(0.0, 64.0, 0.0);
        player.connection.teleport(0.0, 64.0, 0.0, player.getYRot(), player.getXRot());
        return true;
    }

    public static void buildOnce(ServerLevel level, Station station) {
        StationBuildData data = StationBuildData.get(level);
        if (data.isBuilt()) return;

        StructureTemplate template = level.getServer().getStructureManager()
                .get(station.getTemplate()).orElse(null);
        if (template == null) {
            StellarStory.getLogger().error("Station {}: template {} not found",
                    station.getName(), station.getTemplate());
            return;
        }

        // Чанки под шаблоном должны быть загружены до расстановки блоков
        BlockPos size = (BlockPos) template.getSize();
        int minCX = ORIGIN.getX() >> 4, maxCX = (ORIGIN.getX() + size.getX()) >> 4;
        int minCZ = ORIGIN.getZ() >> 4, maxCZ = (ORIGIN.getZ() + size.getZ()) >> 4;
        for (int cx = minCX; cx <= maxCX; cx++)
            for (int cz = minCZ; cz <= maxCZ; cz++)
                level.getChunk(cx, cz);

        template.placeInWorld(level, ORIGIN, ORIGIN, new StructurePlaceSettings(),
                RandomSource.create(level.getSeed()), 3);

        data.built = true;
        data.setDirty();
    }
}
