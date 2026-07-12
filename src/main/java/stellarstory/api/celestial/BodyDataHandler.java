package stellarstory.api.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import stellarstory.api.celestial.data.AtmosphereData;
import stellarstory.api.celestial.data.PhysicalData;

public record BodyDataHandler(
        PhysicalData physicalData,
        AtmosphereData atmosphereData
) {
    public static final BodyDataHandler DEFAULT = new BodyDataHandler(PhysicalData.DEFAULT, AtmosphereData.DEFAULT);

    public static final Codec<BodyDataHandler> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PhysicalData.CODEC.fieldOf("physical").orElse(PhysicalData.DEFAULT).forGetter(BodyDataHandler::physicalData),
            AtmosphereData.CODEC.fieldOf("atmosphere").orElse(AtmosphereData.DEFAULT).forGetter(BodyDataHandler::atmosphereData)
    ).apply(instance, BodyDataHandler::new));

}
