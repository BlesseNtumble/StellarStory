package stellarstory.api.celestial.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PhysicalData(
        long dayLength,
        float gravity
) {
    public static final PhysicalData DEFAULT = new PhysicalData(24000L, 1.0F);
    public static final Codec<PhysicalData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("day_length").orElse(24000L).forGetter(PhysicalData::dayLength),
            Codec.FLOAT.fieldOf("gravity").orElse(1.0F).forGetter(PhysicalData::gravity)
    ).apply(instance, PhysicalData::new));
}
