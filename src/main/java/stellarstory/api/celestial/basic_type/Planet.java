package stellarstory.api.celestial.basic_type;


import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.IChildBody;

import java.util.Optional;
import java.util.Set;

public class Planet extends CelestialBody {

    private Star parentStar;
    private Set<IChildBody> childs = Sets.newHashSet();

    public static final Codec<Planet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(CelestialBody::getName),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(CelestialBody::getIcon)
    ).apply(instance, (name, icon) -> {
        Planet.Builder builder = Planet.builder()
                .name(name)
                .icon(icon);

        return builder.build();
    }));

    public Planet(Builder builder) {
        super(builder);
        this.parentStar = builder.parentStar;
    }

    public void setParentStar(Star parentStar) {
        this.parentStar = parentStar;
    }

    public Optional<Star> getParentStar() {
        return Optional.ofNullable(parentStar);
    }

    public ImmutableSet<IChildBody> getChilds() {
        return ImmutableSet.copyOf(childs);
    }

    public void addChild(IChildBody child) { this.childs.add(child); }

    @Override
    public Codec<? extends CelestialBody> getCodec() {
        return CODEC;
    }

    // Builder для Planet
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends CelestialBody.Builder<Builder> {
        private Star parentStar;

        public Builder parentStar(Star parentStar) {
            this.parentStar = parentStar;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public Planet build() {
            return new Planet(this);
        }
    }
}
