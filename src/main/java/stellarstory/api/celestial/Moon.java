package stellarstory.api.celestial;

import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.registries.RegistryObject;
import stellarstory.common.registers.StellarDimensions;

public class Moon extends CelestialBody implements IChildBody{

    private Planet parent_planet;

    public Moon(String name) {
        super(Properties.of(name));
    }

    @Override
    public Moon setParentPlanet(Planet planet) {
        this.parent_planet = planet;
        this.parent_planet.getParentStar().getParentStarSystem().addChild(this);
        return this;
    }

    @Override
    public Planet getParentPlanet() {
        return parent_planet;
    }
}
