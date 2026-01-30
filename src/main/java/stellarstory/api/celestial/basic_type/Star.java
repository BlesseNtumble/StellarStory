package stellarstory.api.celestial.basic_type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.StarSystem;

import java.util.*;

public class Star extends CelestialBody {

    public static final Codec<Star> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Star::getName),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(Star::getIcon),
            ResourceLocation.CODEC.listOf().fieldOf("planets").forGetter(Star::getPlanetReferences)
    ).apply(instance, (name, icon, planets) -> {
        Star.Builder builder = Star.builder().name(name).icon(icon);
        Star star = builder.build();
        star.planetsReferences = planets;
        return star;
    }));

    private StarSystem parentSystem;
    private List<ResourceLocation> planetsReferences = new ArrayList<>();
    private List<Planet> planets;

    public Star(Builder builder) {
        super(builder);
        this.parentSystem = builder.parent_system;
        this.planets = builder.planets;
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

    public void resolvePlanets(Map<ResourceLocation, Planet> planetMap) {

        for (ResourceLocation starReference : planetsReferences) {
            Planet planet = planetMap.get(starReference);
            if (planet != null) {
                planet.setParentStar(this);
                planets.add(planet);
            } else {
                System.err.println("Star not found: " + starReference);
            }
        }
    }

    public static class Builder extends CelestialBody.Builder<Builder> {
        private StarSystem parent_system;
        private List<Planet> planets = new ArrayList<>();

        public Builder parentSystem(StarSystem parent_system) {
            this.parent_system = parent_system;
            return this;
        }

        public Builder withPlanet(Planet planet) {
            this.planets.add(planet);
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
