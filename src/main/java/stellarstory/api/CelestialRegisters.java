package stellarstory.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.network.PacketDistributor;
import stellarstory.StellarStory;
import stellarstory.api.celestial.*;
import stellarstory.api.celestial.StarSystem;
import stellarstory.api.celestial.basic_type.*;
import stellarstory.common.capability.PlayerCapability;
import stellarstory.common.init.StellarCapabilities;
import stellarstory.common.level.StationBuildData;
import stellarstory.network.NetworkHandler;
import stellarstory.network.packet.client.SyncStationUnlockPacket;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CelestialRegisters {


    private static final Map<ResourceLocation, StarSystem> STAR_SYSTEMS = Maps.newHashMap();
    private static final Map<ResourceLocation, Station> STATIONS = Maps.newHashMap();
    private static final Map<ResourceLocation, CelestialBody> BODIES = Maps.newHashMap();
    /** id измерения -> тело. Кэширует и «пустые» измерения, чтобы не сканировать системы каждый тик. */
    private static final Map<ResourceLocation, CelestialBody> BY_DIMENSION = Maps.newHashMap();

    /** Возвращается вместо null: isEmpty() == true, isReachable() == false. */
    public static final CelestialBody EMPTY = Star.builder().name(CelestialBody.EMPTY_NAME).build();

    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    /** Центр посадочной платформы mars_station.nbt (локальные 10,1,10 + ORIGIN). */
    private static final Vec3 SPAWN_POINT = new Vec3(10.5D, 65.0D, 10.5D);

    public static void reset() {
        STAR_SYSTEMS.clear();
        STATIONS.clear();
        BODIES.clear();
        BY_DIMENSION.clear();
    }

    public static void registerStarSystem(ResourceLocation rl, StarSystem star_system){
        STAR_SYSTEMS.put(rl, star_system);
    }

    public static void registerBody(ResourceLocation id, CelestialBody body) {
        BODIES.put(id, body);
        indexByDimension(body);
    }

    public static void registerStation(ResourceLocation id, Station station) {
        STATIONS.put(id, station);
        indexByDimension(station);
    }

    private static void indexByDimension(CelestialBody body) {
        CelestialBody previous = BY_DIMENSION.putIfAbsent(body.getDimensionId(), body);
        if (previous != null && previous != body) {
            StellarStory.getLogger().error("Dimension {} is claimed by both '{}' and '{}'",
                    body.getDimensionId(), previous.getName(), body.getName());
        }
    }

    public static Collection<Station> getStations() {
        return STATIONS.values();
    }

    public static CelestialBody getCelestialBodyFromPlayer(Player player) {
        return getCelestialBodyFromLevel(player.level());
    }

    public static CelestialBody getCelestialBodyFromLevel(Level level) {
        return getCelestialBodyByDimension(level.dimension().location());
    }

    /**
     * Тело, привязанное к измерению. Никогда не возвращает null — вместо отсутствующего
     * тела возвращается {@link #EMPTY}.
     */
    public static CelestialBody getCelestialBodyByDimension(ResourceLocation dimension) {
        return BY_DIMENSION.computeIfAbsent(dimension,
                id -> {
                    CelestialBody body = getCelestialBody(id.getPath());
                    return body != null ? body : EMPTY;
                });
    }

    /**
     * dimension_type.effects совпадает с id измерения, поэтому mixin по времени суток
     * резолвит тело этим же методом.
     */
    public static CelestialBody getCelestialBodyByEffects(ResourceLocation effects) {
        return getCelestialBodyByDimension(effects);
    }

    /** Тело, чья гравитация действует в измерении: сама планета либо планета-родитель луны/станции. */
    public static Planet getGravityOwner(Level level) {
        CelestialBody body = getCelestialBodyFromLevel(level);
        if (body instanceof Planet planet) {
            return planet;
        }
        if (body instanceof IChildBody child) {
            return child.getParentPlanet();
        }
        return null;
    }

    public static CelestialBody getCelestialBody(String name) {
        if (name == null) {
            return null;
        }

        CelestialBody direct = BODIES.get(StellarStory.prefix(name.toLowerCase(Locale.ROOT)));
        if (direct != null) {
            return direct;
        }

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

    /**
     * Открывает станцию игроку: пишет в PlayerCapability (сохраняется в NBT,
     * переживает перезаход и смерть), строит станцию и синхронизирует клиент.
     */
    public static void unlock(ServerPlayer player, Station station) {
        player.getCapability(StellarCapabilities.PLAYER_CAPABILITY).ifPresent(cap -> {
            if (cap.unlockStation(station.getName())) {
                // Синхронизируем с клиентом только при реальном изменении
                NetworkHandler.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
                        new SyncStationUnlockPacket(station.getName(), true));
            }
        });

        ServerLevel level = player.server.getLevel(station.getDimensionKey());
        if (level != null) buildOnce(level, station);
    }

    /** Открыта ли станция игроку. Работает на обеих сторонах (читает PlayerCapability). */
    public static boolean isStationUnlocked(Player player, Station station) {
        return player.getCapability(StellarCapabilities.PLAYER_CAPABILITY)
                .map(cap -> cap.isStationUnlocked(station.getName()))
                .orElse(false);
    }

    /** Можно ли открыть станцию: она ещё не открыта и игрок ещё не находится на ней. */
    public static boolean canUnlock(ServerPlayer player, Station station) {
        if (station == null) {
            return false;
        }
        return !isStationUnlocked(player, station);
    }

    public static boolean teleportToStation(ServerPlayer player, Station station) {
        ServerLevel level = player.server.getLevel(station.getDimensionKey());
        if (level == null) { // измерения нет в датапаке
            StellarStory.getLogger().error("Station {} has no dimension {}", station.getName(), station.getDimensionId());
            return false;
        }

        // Строим до перехода: игрок появится уже на готовой постройке,
        // и placeInWorld гарантированно выполняется на серверном потоке.
        boolean builtNow = buildOnce(level, station);

        player.changeDimension(level, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel destLevel,
                                            Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(SPAWN_POINT, Vec3.ZERO, entity.getYRot(), entity.getXRot());
            }
        });

        if (builtNow) {
            StellarStory.getLogger().info("Station {} built in {}", station.getName(), station.getDimensionId());
        }
        return true;
    }

    /**
     * Размещает постройку станции в её измерении один раз.
     * Вызывать только с серверного потока.
     *
     * @return true, если постройка была размещена именно в этом вызове
     */
    public static boolean buildOnce(ServerLevel level, Station station) {
        if (station.getTemplate() == null) {
            StellarStory.getLogger().error("Station {} has no template", station.getName());
            return false;
        }

        StationBuildData data = StationBuildData.get(level);
        if (data.isBuiltFor(station.getTemplate())) {
            return false;
        }

        StructureTemplate template = level.getServer().getStructureManager()
                .get(station.getTemplate()).orElse(null);
        if (template == null) {
            StellarStory.getLogger().error("Station {}: template {} not found",
                    station.getName(), station.getTemplate());
            return false;
        }

        // Чанки под шаблоном должны быть загружены до расстановки блоков
        Vec3i size = template.getSize();
        int minCX = ORIGIN.getX() >> 4, maxCX = (ORIGIN.getX() + size.getX()) >> 4;
        int minCZ = ORIGIN.getZ() >> 4, maxCZ = (ORIGIN.getZ() + size.getZ()) >> 4;
        for (int cx = minCX; cx <= maxCX; cx++)
            for (int cz = minCZ; cz <= maxCZ; cz++)
                level.getChunk(cx, cz);

        template.placeInWorld(level, ORIGIN, ORIGIN, new StructurePlaceSettings(),
                RandomSource.create(level.getSeed()), 3);

        data.markBuilt(station.getTemplate());
        return true;
    }
}
