package stellarstory.api.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Barycenter — the center of mass of a sub-system (e.g. a double star).
 *
 * <p>It is a purely mathematical point, not a physical body: it is never
 * rendered on the map and cannot be selected.  Stars linked to a barycenter
 * orbit around its local position inside the star system.</p>
 */
public class Barycenter {

    private final String name;
    private final float mass;
    private final double x;
    private final double y;
    private final double z;

    public static final Codec<Barycenter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Barycenter::getName),
            Codec.FLOAT.fieldOf("mass").orElse(1.0F).forGetter(Barycenter::getMass),
            Codec.DOUBLE.fieldOf("x").orElse(0.0).forGetter(Barycenter::getX),
            Codec.DOUBLE.fieldOf("y").orElse(0.0).forGetter(Barycenter::getY),
            Codec.DOUBLE.fieldOf("z").orElse(0.0).forGetter(Barycenter::getZ)
    ).apply(instance, Barycenter::new));

    public Barycenter(String name, float mass, double x, double y, double z) {
        this.name = name;
        this.mass = mass;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getName() {
        return name;
    }

    public float getMass() {
        return mass;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    @Override
    public String toString() {
        return name;
    }
}