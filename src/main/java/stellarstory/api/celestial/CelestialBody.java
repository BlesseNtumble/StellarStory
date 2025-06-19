package stellarstory.api.celestial;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.registries.RegistryObject;
import stellarstory.StellarRegistries;
import stellarstory.StellarStory;
import stellarstory.api.celestial.data.AtmosphereData;
import stellarstory.api.celestial.data.PhysicalData;
import stellarstory.common.celestials.solarsystems.world.MoonChunkGenerator;

import java.util.OptionalLong;

public abstract class CelestialBody {

    private final CelestialBody.Properties properties;
    public RegistryObject<DimensionType> CELESTIAL_DIM_TYPE;
    public RegistryObject<Codec<MoonChunkGenerator>> CELESTIAL_CHUNK_GENERATOR;
    public ResourceKey<Level> CELESTIAL_LEVEL;

    public static CelestialBody EMPTY = new CelestialBody(Properties.of("EMPTY")) {};

    public CelestialBody(CelestialBody.Properties properties){

        this.properties = properties;
    }

    public String getName() {
        return this.properties.name;
    }

    public ResourceLocation getIcon() {return this.properties.icon; }

    public CelestialBody setReachable() {
        CELESTIAL_DIM_TYPE = StellarRegistries.DIMENSION_TYPES.register( getName() + "_type", this::dimType);
        CELESTIAL_CHUNK_GENERATOR = StellarRegistries.CHUNK_GENERATORS.register(getName() + "_chunk_generator", () -> MoonChunkGenerator.CODEC);

        CELESTIAL_LEVEL = ResourceKey.create(Registry.DIMENSION_REGISTRY, StellarStory.prefix(getName()));

        return this;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }


    public CelestialBody.Properties getProperties() {
        return this.properties;
    }

    public CelestialBody setPhysicalData(PhysicalData data) {
        this.properties.setPhysicalData(data);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return this.getName() == ((CelestialBody)obj).getName();
    }

    public boolean isReachable() { return !isEmpty() && CELESTIAL_DIM_TYPE != null; }

    @Override
    public String toString() {
        return this.getName();
    }

    private DimensionType dimType() {
        return new DimensionType(
                OptionalLong.empty(), //time
                true, //skylight
                false, //ceiling
                false, //ultrawarm
                false, //natural
                0.5D, //coordinate scale
                false, //bed works
                true, //respawn anchor works
                -32, // Minimum Y Level
                32 + 256, // Height + Min Y = Max Y
                32 + 256, // Logical Height
                BlockTags.INFINIBURN_OVERWORLD, //infiburn
                StellarStory.prefix(this.getName()), // DimensionRenderInfo
                0f, // Wish this could be set to -0.05 since it'll make the world truly blacked out if an area is not sky-lit (see: Dark Forests) Sadly this also messes up night vision so it gets 0
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 7), 7)
        );
    }

    public static class Properties {
        String name;
        public ResourceLocation icon;
        PhysicalData physicalData;
        AtmosphereData atmosphereData;

        private Properties(String name) {
            this.name = name;
        }

        public static Properties of(String name) {
            return new Properties(name);
        }

        public Properties setIcon(ResourceLocation icon) {
            this.icon = icon;
            return this;
        }

        public Properties setPhysicalData(PhysicalData data) {
            this.physicalData = data;
            return this;
        }

        public Properties setAtmosphereData(AtmosphereData data) {
            this.atmosphereData = data;
            return this;
        }

        public PhysicalData getOrCreatePhysicalData() {
            if(this.physicalData == null)
                return PhysicalData.Builder.create();

            return this.physicalData;
        }

        public AtmosphereData getOrCreateAtmosphereData() {
            if(this.atmosphereData == null)
                return AtmosphereData.Builder.create();

            return this.atmosphereData;
        }
    }
}
