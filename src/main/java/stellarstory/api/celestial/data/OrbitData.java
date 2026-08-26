package stellarstory.api.celestial.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Physical orbital elements of a celestial body.
 *
 * <p>Describes how the body moves around its parent (star, planet or barycenter):
 * semi-major axis, period, phase, eccentricity, inclination and ascending node.</p>
 *
 * @param semiMajorAxis semi-major axis (a) in map units — radius for circular orbits
 * @param period        orbital period in game ticks (24000 ticks = 1 in-game day)
 * @param phase         initial mean anomaly (radians) at t = 0
 * @param eccentricity  orbital eccentricity, 0..1
 * @param inclination   tilt of the orbital plane in degrees
 * @param ascendingNode longitude of the ascending node in degrees
 */
public record OrbitData(
        float semiMajorAxis,
        long period,
        float phase,
        float eccentricity,
        float inclination,
        float ascendingNode
) {
    public static final OrbitData DEFAULT = new OrbitData(30.0F, 24000L, 0.0F, 0.0F, 0.0F, 0.0F);

    public static final Codec<OrbitData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("semi_major_axis").orElse(30.0F).forGetter(OrbitData::semiMajorAxis),
            Codec.LONG.fieldOf("period").orElse(24000L).forGetter(OrbitData::period),
            Codec.FLOAT.fieldOf("phase").orElse(0.0F).forGetter(OrbitData::phase),
            Codec.FLOAT.fieldOf("eccentricity").orElse(0.0F).forGetter(OrbitData::eccentricity),
            Codec.FLOAT.fieldOf("inclination").orElse(0.0F).forGetter(OrbitData::inclination),
            Codec.FLOAT.fieldOf("ascending_node").orElse(0.0F).forGetter(OrbitData::ascendingNode)
    ).apply(instance, OrbitData::new));
}
