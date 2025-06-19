package stellarstory.api.celestial;

public interface IChildBody {
    IChildBody setParentPlanet(Planet planet);
    Planet getParentPlanet();
}
