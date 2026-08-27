package stellarstory.api.celestial.basic_type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.StarSystem;
import stellarstory.api.celestial.basic_type.Asteroid;
import stellarstory.api.celestial.basic_type.Comet;
import stellarstory.api.celestial.data.OrbitData;
import stellarstory.api.celestial.data.OrbitVisual;

import java.util.*;

public class Star extends CelestialBody {

    /** Спектральные классы звёзд с их характерными цветами. */
    public enum SpectralClass {
        O("O", 1.0F, 0.6F, 0.8F, "Голубой"),
        B("B", 0.8F, 0.85F, 1.0F, "Голубовато-белый"),
        A("A", 0.95F, 0.95F, 1.0F, "Белый"),
        F("F", 1.0F, 0.98F, 0.85F, "Жёлто-белый"),
        G("G", 1.0F, 0.92F, 0.6F, "Жёлтый"),
        K("K", 1.0F, 0.75F, 0.4F, "Оранжевый"),
        M("M", 1.0F, 0.4F, 0.25F, "Красный");

        public final String symbol;
        public final float r, g, b;
        public final String displayName;

        SpectralClass(String symbol, float r, float g, float b, String displayName) {
            this.symbol = symbol;
            this.r = r;
            this.g = g;
            this.b = b;
            this.displayName = displayName;
        }

        /** Получить цвет звезды по символу класса. */
        public static SpectralClass fromSymbol(String symbol) {
            for (SpectralClass cls : values()) {
                if (cls.symbol.equals(symbol)) return cls;
            }
            // По умолчанию — жёлтая звезда (как Солнце)
            return G;
        }
    }

    public static final Codec<Star> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Star::getName),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(Star::getIcon),
            ResourceLocation.CODEC.listOf().fieldOf("planets").forGetter(Star::getPlanetReferences),
            ResourceLocation.CODEC.listOf().fieldOf("asteroids").orElse(List.of()).forGetter(Star::getAsteroidReferences),
            ResourceLocation.CODEC.listOf().fieldOf("comets").orElse(List.of()).forGetter(Star::getCometReferences),
            ResourceLocation.CODEC.fieldOf("barycenter").orElse(ResourceLocation.tryParse("null")).forGetter(Star::getBarycenterRefRaw),
            Codec.STRING.fieldOf("spectral_class").orElse("G").forGetter(s -> s.spectralClass != null ? s.spectralClass.symbol : "G"),
            OrbitData.CODEC.fieldOf("orbit").orElse(OrbitData.DEFAULT).forGetter(Star::getOrbit),
            OrbitVisual.CODEC.fieldOf("orbit_visual").orElse(OrbitVisual.DEFAULT).forGetter(Star::getOrbitVisual)
    ).apply(instance, (name, icon, planets, asteroids, comets, barycenterRef, spectralClassSymbol, orbit, orbitVisual) -> {
        Star.Builder builder = Star.builder().name(name).icon(icon).orbit(orbit).orbitVisual(orbitVisual);
        if (barycenterRef != null && !barycenterRef.getPath().equals("null")) {
            builder.barycenterReference(barycenterRef);
        }
        Star star = builder.build();
        star.planetsReferences = planets;
        star.asteroidsReferences = asteroids;
        star.cometsReferences = comets;
        star.spectralClass = SpectralClass.fromSymbol(spectralClassSymbol);
        return star;
    }));

    private StarSystem parentSystem;
    private List<ResourceLocation> planetsReferences = new ArrayList<>();
    private List<Planet> planets;
    private List<ResourceLocation> asteroidsReferences = new ArrayList<>();
    private List<ResourceLocation> cometsReferences = new ArrayList<>();
    private List<Asteroid> asteroids;
    private List<Comet> comets;
    private SpectralClass spectralClass = SpectralClass.G;

    public Star(Builder builder) {
        super(builder);
        this.parentSystem = builder.parent_system;
        this.planets = builder.planets;
        this.asteroids = new ArrayList<>();
        this.comets = new ArrayList<>();
        this.spectralClass = builder.spectralClass;
    }


    public void setParentStarSystem(StarSystem starSystem) {
        this.parentSystem = starSystem;
    }

    public Optional<StarSystem> getParentSystem() {
        return Optional.ofNullable(parentSystem);
    }

    public List<Planet> getPlanets() {
        return Collections.unmodifiableList(planets);
    }

    public void addPlanet(Planet planet) {
        planets.add(planet);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Codec<? extends CelestialBody> getCodec() {
        return CODEC;
    }

    public List<ResourceLocation> getPlanetReferences() {
        return Collections.unmodifiableList(planetsReferences);
    }

    public List<ResourceLocation> getAsteroidReferences() {
        return Collections.unmodifiableList(asteroidsReferences);
    }

    public List<ResourceLocation> getCometReferences() {
        return Collections.unmodifiableList(cometsReferences);
    }

    public List<Asteroid> getAsteroids() {
        return Collections.unmodifiableList(asteroids);
    }

    public List<Comet> getComets() {
        return Collections.unmodifiableList(comets);
    }

    public SpectralClass getSpectralClass() {
        return spectralClass;
    }

    public float[] getStarColor() {
        return new float[]{spectralClass.r, spectralClass.g, spectralClass.b};
    }

    ResourceLocation getBarycenterRefRaw() {
        return barycenterReference;
    }

    public void resolvePlanets(Map<ResourceLocation, Planet> planetMap) {

        for (ResourceLocation starReference : planetsReferences) {
            Planet planet = planetMap.get(starReference);
            if (planet != null) {
                planet.setParentStar(this);
                planets.add(planet);
            } else {
                System.err.println("Planet not found: " + starReference);
            }
        }
    }

    public void resolveAsteroids(Map<ResourceLocation, Asteroid> asteroidMap) {
        for (ResourceLocation reference : asteroidsReferences) {
            Asteroid asteroid = asteroidMap.get(reference);
            if (asteroid != null) {
                asteroids.add(asteroid);
            } else {
                System.err.println("Asteroid not found: " + reference);
            }
        }
    }

    public void resolveComets(Map<ResourceLocation, Comet> cometMap) {
        for (ResourceLocation reference : cometsReferences) {
            Comet comet = cometMap.get(reference);
            if (comet != null) {
                comets.add(comet);
            } else {
                System.err.println("Comet not found: " + reference);
            }
        }
    }

    public static class Builder extends CelestialBody.Builder<Builder> {
        private StarSystem parent_system;
        private List<Planet> planets = new ArrayList<>();
        private List<Asteroid> asteroids = new ArrayList<>();
        private List<Comet> comets = new ArrayList<>();
        private SpectralClass spectralClass = SpectralClass.G;

        public Builder parentSystem(StarSystem parent_system) {
            this.parent_system = parent_system;
            return this;
        }

        public Builder withPlanet(Planet planet) {
            this.planets.add(planet);
            return this;
        }

        public Builder withAsteroid(Asteroid asteroid) {
            if (this.asteroids == null) this.asteroids = new ArrayList<>();
            this.asteroids.add(asteroid);
            return this;
        }

        public Builder withComet(Comet comet) {
            if (this.comets == null) this.comets = new ArrayList<>();
            this.comets.add(comet);
            return this;
        }

        public Builder spectralClass(SpectralClass spectralClass) {
            this.spectralClass = spectralClass;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public Star build() {
            return new Star(this);
        }
    }
}
