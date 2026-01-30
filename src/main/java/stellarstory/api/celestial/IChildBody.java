package stellarstory.api.celestial;

import stellarstory.api.celestial.basic_type.Planet;

public interface IChildBody {
    IChildBody setParentPlanet(Planet planet);
    Planet getParentPlanet();
}
