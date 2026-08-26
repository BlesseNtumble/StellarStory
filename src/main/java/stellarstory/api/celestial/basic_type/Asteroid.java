package stellarstory.api.celestial.basic_type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.data.OrbitData;
import stellarstory.api.celestial.data.OrbitVisual;

import java.util.Optional;

/**
 * An asteroid — a small rocky body that can orbit a {@link Star}
 * (asteroid belt) or a {@link Planet} (like Phobos or Deimos around Mars).
 */
public class Asteroid extends CelestialBody {

    private Star parentStar;
    private Planet parentPlanet;

    public static final Codec<Asteroid> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(CelestialBody::getName),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(CelestialBody::getIcon),
            OrbitData.CODEC.fieldOf("orbit").orElse(OrbitData.DEFAULT).forGetter(CelestialBody::getOrbit),
            OrbitVisual.CODEC.fieldOf("orbit_visual").orElse(OrbitVisual.DEFAULT).forGetter(CelestialBody::getOrbitVisual)
    ).apply(instance, (name, icon, orbit, orbitVisual) ->
            Asteroid.builder().name(name).icon(icon).orbit(orbit).orbitVisual(orbitVisual).build()));

    public Asteroid(Builder builder) {
        super(builder);
    }

    public void setParentStar(Star parentStar) {
        this.parentStar = parentStar;
    }

    public Optional<Star> getParentStar() {
        return Optional.ofNullable(parentStar);
    }

    public void setParentPlanet(Planet parentPlanet) {
        this.parentPlanet = parentPlanet;
    }

    public Optional<Planet> getParentPlanet() {
        return Optional.ofNullable(parentPlanet);
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
        protected Builder self() {
            return this;
        }

        @Override
        public Asteroid build() {
            return new Asteroid(this);
        }
    }
}