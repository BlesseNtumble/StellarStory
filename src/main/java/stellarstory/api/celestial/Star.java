package stellarstory.api.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public class Star extends CelestialBody {

    private StarSystem parent_system;

    public static final Codec<Star> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Star::getName)
    ).apply(instance, Star::new));

    public Star(String name) {
        super(Properties.of(name));
    }

    public Star setParentStarSystem(StarSystem system) {
        this.parent_system = system;
        this.parent_system.addStar(this);
        return this;
    }

    public StarSystem getParentStarSystem() {
        return this.parent_system;
    }


}
