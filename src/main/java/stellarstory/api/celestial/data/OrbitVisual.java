package stellarstory.api.celestial.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Visual parameters for rendering an orbit in the space map.
 *
 * <p>Unlike {@link OrbitData}, which describes the physical orbit
 * (semi-major axis, period, etc.), this record contains only
 * rendering hints: how thick the orbit line is, how wide the
 * asteroid scatter is, and how fast the body spins.</p>
 */
public record OrbitVisual(
        /** Radial scatter for asteroid belts (map units). 0 = single rock. */
        float spread,
        /** Vertical scatter for asteroid belts (map units). 0 = flat plane. */
        float thickness,
        /** Rendered orbit line width in pixels (1..4). */
        int orbitWidth,
        /** Body rotation speed (radians per tick unit). */
        float spinSpeed,
        /** Body size (cube edge length) on the map. */
        float bodySize
) {
    public static final OrbitVisual DEFAULT = new OrbitVisual(0.0F, 0.0F, 1, 1.0F, 1.0F);

    public static final Codec<OrbitVisual> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("spread").orElse(0.0F).forGetter(OrbitVisual::spread),
            Codec.FLOAT.fieldOf("thickness").orElse(0.0F).forGetter(OrbitVisual::thickness),
            Codec.INT.fieldOf("orbit_width").orElse(1).forGetter(OrbitVisual::orbitWidth),
            Codec.FLOAT.fieldOf("spin_speed").orElse(1.0F).forGetter(OrbitVisual::spinSpeed),
            Codec.FLOAT.fieldOf("body_size").orElse(1.0F).forGetter(OrbitVisual::bodySize)
    ).apply(instance, OrbitVisual::new));
}