package stellarstory.api.celestial;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class Planet extends CelestialBody {

    private Star parent_star;
    public Planet(String name) {
        super(Properties.of(name));
    }

    public Planet setParentStar(Star star) {
        this.parent_star = star;
        this.parent_star.getParentStarSystem().addPlanet(this);
        return this;
    }

    public Star getParentStar() {
        return this.parent_star;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", this.getName(), this.getParentStar().getName());
    }

}
