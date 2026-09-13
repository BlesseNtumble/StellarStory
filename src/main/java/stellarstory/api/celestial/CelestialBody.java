package stellarstory.api.celestial;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import stellarstory.StellarStory;
import stellarstory.api.celestial.data.AtmosphereData;
import stellarstory.api.celestial.data.OrbitData;
import stellarstory.api.celestial.data.OrbitVisual;
import stellarstory.api.celestial.data.PhysicalData;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class CelestialBody implements ICodecBody {

    /** Имя заглушки, которую реестры возвращают вместо null. */
    public static final String EMPTY_NAME = "empty";

    protected String name;
    protected ResourceLocation icon;
    protected final BodyDataHandler bodyDataHandler;
    protected boolean isReachable;

    // Physical orbit around the parent body (star, planet or barycenter)
    protected OrbitData orbit = OrbitData.DEFAULT;
    // Visual rendering parameters for the orbit
    protected OrbitVisual orbitVisual = OrbitVisual.DEFAULT;
    // Nullable — the barycenter this body orbits around instead of its direct parent
    protected ResourceLocation barycenterReference;
    protected Barycenter barycenter;

    public CelestialBody(Builder<?> builder) {
        this.name = builder.name;
        this.icon = builder.icon;
        this.isReachable = builder.reachable;
        this.bodyDataHandler = builder.buildBodyData();
        this.orbit = builder.orbit;
        this.orbitVisual = builder.orbitVisual;
        this.barycenterReference = builder.barycenterReference;
    }

    public String getName() {
        return this.name;
    }

    public ResourceLocation getIcon() {return this.icon; }

    /**
     * Идентификатор измерения, в котором находится это тело.
     * Соглашение по умолчанию: stellarstory:&lt;name в нижнем регистре&gt;.
     * Тела с собственным измерением (станции) переопределяют метод.
     */
    public ResourceLocation getDimensionId() {
        String path = this.name == null ? EMPTY_NAME : this.name.toLowerCase(Locale.ROOT);
        return ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, path);
    }

    public ResourceKey<Level> getDimensionKey() {
        return ResourceKey.create(Registries.DIMENSION, getDimensionId());
    }

    @Override
    public abstract Codec<? extends CelestialBody> getCodec();

    /**
     * Возвращает true для заглушки (см. CelestialRegisters#EMPTY) и для тел без имени.
     * Не бросает NPE, поэтому безопасна в горячих тиковых обработчиках.
     */
    public boolean isEmpty() {
        return this.name == null || this.name.isEmpty() || EMPTY_NAME.equals(this.name);
    }

    public boolean isReachable() { return isReachable; }

    public BodyDataHandler getBodyDataHandler() {
        return bodyDataHandler;
    }

    // Getters for orbit data
    public OrbitData getOrbit() {
        return orbit;
    }

    public OrbitVisual getOrbitVisual() {
        return orbitVisual;
    }

    public ResourceLocation getBarycenterReference() {
        return barycenterReference;
    }

    public Optional<Barycenter> getBarycenter() {
        return Optional.ofNullable(barycenter);
    }

    public void resolveBarycenter(Map<ResourceLocation, Barycenter> barycenterMap) {
        if (barycenterReference != null) {
            Barycenter bc = barycenterMap.get(barycenterReference);
            if (bc != null) {
                this.barycenter = bc;
            }
        }
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

        // orbit
        protected OrbitData orbit = OrbitData.DEFAULT;
        protected OrbitVisual orbitVisual = OrbitVisual.DEFAULT;
        protected ResourceLocation barycenterReference;

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

        public T orbit(OrbitData orbit) {
            this.orbit = orbit;
            return self();
        }

        public T orbitVisual(OrbitVisual orbitVisual) {
            this.orbitVisual = orbitVisual;
            return self();
        }

        public T barycenterReference(ResourceLocation barycenterReference) {
            this.barycenterReference = barycenterReference;
            return self();
        }

        protected BodyDataHandler buildBodyData() {
            return new BodyDataHandler(new PhysicalData(dayLength, gravity), new AtmosphereData(temperature, temperatureModificator, temperatureOffset));
        }

        protected abstract T self();

        public abstract CelestialBody build();
    }
}
