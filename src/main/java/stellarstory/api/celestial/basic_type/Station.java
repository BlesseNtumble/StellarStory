package stellarstory.api.celestial.basic_type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import stellarstory.api.celestial.BodyDataHandler;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.IChildBody;
import stellarstory.api.celestial.data.OrbitData;
import stellarstory.api.celestial.data.OrbitVisual;

import java.util.Optional;

public class Station extends CelestialBody implements IChildBody {

    private ResourceLocation dimension;
    private ResourceLocation template;
    private Planet parentPlanet;
    private Moon parentMoon;

    public static final Codec<Station> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    // Наследуем от CelestialBody
                    Codec.STRING.fieldOf("name").forGetter(CelestialBody::getName),
                    ResourceLocation.CODEC.fieldOf("icon").forGetter(CelestialBody::getIcon),
                    Codec.BOOL.fieldOf("reachable").forGetter(CelestialBody::isReachable),
                    BodyDataHandler.CODEC.fieldOf("data")
                            .orElse(BodyDataHandler.DEFAULT)
                            .forGetter(CelestialBody::getBodyDataHandler),
                    OrbitData.CODEC.fieldOf("orbit").orElse(OrbitData.DEFAULT)
                            .forGetter(CelestialBody::getOrbit),
                    OrbitVisual.CODEC.fieldOf("orbit_visual").orElse(OrbitVisual.DEFAULT)
                            .forGetter(CelestialBody::getOrbitVisual),

                    // Свои поля
                    ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template),
                    ResourceLocation.CODEC.optionalFieldOf("dimension").forGetter(s -> Optional.ofNullable(s.dimension))
            ).apply(instance, (name, icon, reachable, data, orbit, orbitVisual, template, dimension) -> {
                var builder = Station.builder()
                        .name(name)
                        .icon(icon)
                        .reachable(reachable)
                        .orbit(orbit)
                        .orbitVisual(orbitVisual);

                // BodyDataHandler содержит gravity и temperature
                var physical = data.physicalData();
                var atm = data.atmosphereData();
                if (physical != null) builder.gravity(physical.gravity());
                if (atm != null) builder.temperature(atm.temperature());

                Station station = builder.build();
                station.template = template;
                station.dimension = dimension.orElse(null);
                return station;
            })
    );

    public Station(Builder builder) {
        super(builder);
    }

    @Override
    public Station setParentPlanet(Planet planet) {
        this.parentPlanet = planet;
        return this;
    }

    @Override
    public Planet getParentPlanet() {
        return parentPlanet;
    }

    @Override
    public Station setParentMoon(Moon moon) {
        this.parentMoon = moon;
        return this;
    }

    @Override
    public Moon getParentMoon() {
        return parentMoon;
    }

    // Свой метод
    public ResourceLocation getTemplate() {
        return template;
    }

    @Override
    public ResourceLocation getDimensionId() {
        return dimension != null ? dimension : super.getDimensionId(); // stellarstory:<name>
    }

    @Override
    public Codec<? extends CelestialBody> getCodec() {
        return CODEC;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends CelestialBody.Builder<Builder> {
        @Override
        protected Builder self() { return this; }
        @Override
        public Station build() { return new Station(this); }
    }
}