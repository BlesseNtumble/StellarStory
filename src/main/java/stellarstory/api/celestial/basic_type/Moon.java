package stellarstory.api.celestial.basic_type;

import com.mojang.serialization.Codec;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.IChildBody;

public class Moon extends CelestialBody implements IChildBody {

    private Planet parent_planet;

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
        return null;
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
