package stellarstory.api.celestial;

import stellarstory.api.celestial.basic_type.Moon;
import stellarstory.api.celestial.basic_type.Planet;

public interface IChildBody {
    IChildBody setParentPlanet(Planet planet);
    Planet getParentPlanet();

    default IChildBody setParentMoon(Moon moon) {
        return this;
    }

    default Moon getParentMoon() {
        return null;
    }
}
