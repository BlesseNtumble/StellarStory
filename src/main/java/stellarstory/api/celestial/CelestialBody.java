package stellarstory.api.celestial;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import stellarstory.StellarStory;
import stellarstory.api.celestial.data.AtmosphereData;
import stellarstory.api.celestial.data.PhysicalData;
import stellarstory.common.init.StellarDimensions;

import java.util.Objects;
import java.util.OptionalLong;

public abstract class CelestialBody implements ICodecBody {

    protected String name;
    protected ResourceLocation icon;

    public CelestialBody(Builder<?> builder) {
        this.name = builder.name;
    }

    public String getName() {
        return this.name;
    }

    public ResourceLocation getIcon() {return this.icon; }

    @Override
    public abstract Codec<? extends CelestialBody> getCodec();

    public CelestialBody setReachable() {
        //CELESTIAL_DIM_TYPE = StellarDimensions.DIMENSION_TYPES.register( getName() + "_type", this::dimType);
        //CELESTIAL_CHUNK_GENERATOR = StellarDimensionsRegistries.CHUNK_GENERATORS.register(getName() + "_chunk_generator", () -> MoonChunkGenerator.CODEC);

        //CELESTIAL_LEVEL = ResourceKey.create(Registries.DIMENSION, StellarStory.prefix(getName()));

        return this;
    }

    public boolean isEmpty() {
        return this.getName().isEmpty();
    }
    //public boolean isReachable() { return !isEmpty() && CELESTIAL_DIM_TYPE != null; }

    @Override
    public boolean equals(Object obj) {
        return Objects.equals(this.getName(), ((CelestialBody) obj).getName());
    }

    @Override
    public String toString() {
        return this.getName();
    }
/*
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
*/
    public static abstract class Builder<T extends Builder<T>> {
        protected String name;
        protected ResourceLocation icon;


        public T name(String name) {
            this.name = name;
            return self();
        }

        public T icon(ResourceLocation icon) {
            this.icon = icon;
            return self();
        }

        protected abstract T self();

        public abstract CelestialBody build();
    }
}
