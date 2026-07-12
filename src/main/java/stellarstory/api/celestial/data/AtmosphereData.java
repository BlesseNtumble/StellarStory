package stellarstory.api.celestial.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record AtmosphereData(
        float temperature,
        float temperatureModificator,
        float temperatureOffset
) {
    public static final float ABSOLUTE_ZERO = -273.15F;

    public static final AtmosphereData DEFAULT = new AtmosphereData(15.0F, 1.0F, 0);
    public static final Codec<AtmosphereData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("temperature").orElse(15.0F).forGetter(AtmosphereData::temperature),
            Codec.FLOAT.fieldOf("temperature_modificator").orElse(1.0F).forGetter(AtmosphereData::temperatureModificator),
            Codec.FLOAT.fieldOf("temperature_offset").orElse(0.0F).forGetter(AtmosphereData::temperatureOffset)
    ).apply(instance, AtmosphereData::new));
}
