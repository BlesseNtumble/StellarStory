package stellarstory.api.celestial.basic_type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import stellarstory.api.celestial.BodyDataHandler;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.IChildBody;
import stellarstory.api.celestial.data.OrbitData;
import stellarstory.api.celestial.data.OrbitVisual;

public class Moon extends CelestialBody implements IChildBody {

    private Planet parent_planet;

    public static final Codec<Moon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(CelestialBody::getName),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(CelestialBody::getIcon),
            Codec.BOOL.fieldOf("reachable").forGetter(CelestialBody::isReachable),
            BodyDataHandler.CODEC.fieldOf("data")
                    .orElse(BodyDataHandler.DEFAULT)
                    .forGetter(CelestialBody::getBodyDataHandler),
            OrbitData.CODEC.fieldOf("orbit").orElse(OrbitData.DEFAULT).forGetter(CelestialBody::getOrbit),
            OrbitVisual.CODEC.fieldOf("orbit_visual").orElse(OrbitVisual.DEFAULT).forGetter(CelestialBody::getOrbitVisual)
    ).apply(instance, (name, icon, reachable, data, orbit, orbitVisual) ->
            Moon.builder().name(name).icon(icon).reachable(reachable).orbit(orbit).orbitVisual(orbitVisual).build()));

    public Moon(Builder builder) {
        super(builder);
    }


    public Moon setParentPlanet(Planet planet) {
        this.parent_planet = planet;
        return this;
    }

    @Override
    public Planet getParentPlanet() {
        return parent_planet;
    }

    @Override
    public Codec<? extends CelestialBody> getCodec() {
        return CODEC;
    }

    public static Moon.Builder builder() {
        return new Moon.Builder();
    }

    public static class Builder extends CelestialBody.Builder<Moon.Builder> {
        private Planet parentPlanet;

        public Moon.Builder parentPlanet(Planet parentPlanet) {
            this.parentPlanet = parentPlanet;
            return this;
        }

        @Override
        protected Moon.Builder self() {
            return this;
        }

        @Override
        public Moon build() {
            return new Moon(this);
        }
    }
}
