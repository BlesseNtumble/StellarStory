package stellarstory.api.celestial.basic_type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.data.OrbitData;
import stellarstory.api.celestial.data.OrbitVisual;

import java.util.Optional;

/**
 * A comet — a small icy body orbiting a {@link Star} on a highly eccentric
 * (elongated) and often inclined orbit.  On the map its orbit is drawn as an
 * ellipse and the body gets a tail pointing away from its parent star.
 */
public class Comet extends CelestialBody {

    private Star parentStar;

    public static final Codec<Comet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(CelestialBody::getName),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(CelestialBody::getIcon),
            OrbitData.CODEC.fieldOf("orbit").orElse(OrbitData.DEFAULT).forGetter(CelestialBody::getOrbit),
            OrbitVisual.CODEC.fieldOf("orbit_visual").orElse(OrbitVisual.DEFAULT).forGetter(CelestialBody::getOrbitVisual)
    ).apply(instance, (name, icon, orbit, orbitVisual) ->
            Comet.builder().name(name).icon(icon).orbit(orbit).orbitVisual(orbitVisual).build()));

    public Comet(Builder builder) {
        super(builder);
    }

    public void setParentStar(Star parentStar) {
        this.parentStar = parentStar;
    }

    public Optional<Star> getParentStar() {
        return Optional.ofNullable(parentStar);
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
        public Comet build() {
            return new Comet(this);
        }
    }
}