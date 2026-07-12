package stellarstory.api.celestial;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import stellarstory.api.celestial.data.AtmosphereData;
import stellarstory.api.celestial.data.PhysicalData;

import java.util.Objects;

public abstract class CelestialBody implements ICodecBody {

    protected String name;
    protected ResourceLocation icon;
    protected final BodyDataHandler bodyDataHandler;
    protected boolean isReachable;

    public CelestialBody(Builder<?> builder) {
        this.name = builder.name;
        this.icon = builder.icon;
        this.isReachable = builder.reachable;
        this.bodyDataHandler = builder.buildBodyData();
    }

    public String getName() {
        return this.name;
    }

    public ResourceLocation getIcon() {return this.icon; }

    @Override
    public abstract Codec<? extends CelestialBody> getCodec();

    public boolean isEmpty() {
        return this.getName().isEmpty() || this.getName() == "null";
    }

    public boolean isReachable() { return isReachable; }

    public BodyDataHandler getBodyDataHandler() {
        return bodyDataHandler;
    }
    @Override
    public boolean equals(Object obj) {
        return Objects.equals(this.getName(), ((CelestialBody) obj).getName());
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public static abstract class Builder<T extends Builder<T>> {
        protected String name;
        protected ResourceLocation icon;
        protected boolean reachable;

        //physical
        protected long dayLength = 24000L;
        protected float gravity = 1.0F;

        //atmposhere
        protected float temperature = 15.0F;
        protected float temperatureModificator = 1.0F;
        protected float temperatureOffset = 0.0F;

        public T name(String name) {
            this.name = name;
            return self();
        }

        public T icon(ResourceLocation icon) {
            this.icon = icon;
            return self();
        }

        public T reachable(boolean reachable) { this.reachable = reachable; return self(); }
        public T dayLength(long val) { this.dayLength = val; return self(); }
        public T gravity(float val) { this.gravity = val; return self(); }

        public T temperature(float val) { this.temperature = val; return self(); }
        public T temperatureModificator(float val) { this.temperatureModificator = val; return self(); }
        public T temperatureOffset(float val) { this.temperatureOffset = val; return self(); }

        protected BodyDataHandler buildBodyData() {
            return new BodyDataHandler(new PhysicalData(dayLength, gravity), new AtmosphereData(temperature, temperatureModificator, temperatureOffset));
        }

        protected abstract T self();

        public abstract CelestialBody build();
    }
}
