package stellarstory.api.celestial;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.ExtraCodecs;
import stellarstory.StellarRegistries;
import stellarstory.StellarStory;
import stellarstory.utils.codec.ImmutableSetCodec;

import java.util.Set;

public class StarSystem {

    // Создание RegistryData
    public static final RegistryAccess.RegistryData<StarSystem> REGISTRY_DATA = new RegistryAccess.RegistryData<>(
            StellarRegistries.STARSYSTEMS_KEY,
            StarSystem.CODEC, null // Указание Codec
    );
    private String galaxy = "milky_way";
    private final String name;
    private Set<Star> stars = Sets.newHashSet();
    private Set<Planet> planets = Sets.newHashSet();
    private Set<IChildBody> childs = Sets.newHashSet();


    public StarSystem(String name) {
        this.name = name;
    }

    public StarSystem(String name, ImmutableSet<Star> stars)
    {
        this.name = name;
        this.stars = stars;
    }

    public void addStar(Star star) {
        this.stars.add(star);
    }

    public ImmutableSet<Star> getStars() {
        return ImmutableSet.copyOf(stars);
    }

    public void addPlanet(Planet planet){
        this.planets.add(planet);
    }

    public ImmutableSet<Planet> getPlanets() { return ImmutableSet.copyOf(planets); }

    public void addChild(IChildBody child){
        this.childs.add(child);
    }

    public ImmutableSet<IChildBody> getChilds() {
        return ImmutableSet.copyOf(childs);
    }

    public Star getMainStar() { return getStars().asList().get(0);}

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", this.name, this.galaxy);
    }

    // CODEC for jsons
    public static final Codec<StarSystem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(StarSystem::getName)
    ).apply(instance, StarSystem::new));
}
