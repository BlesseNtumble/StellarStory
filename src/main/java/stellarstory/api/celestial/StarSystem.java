package stellarstory.api.celestial;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import stellarstory.api.celestial.basic_type.Star;


import java.util.*;

public class StarSystem {

    private String galaxy = "milky_way";
    private String systemName;
    private List<ResourceLocation> starReferences;
    private List<Star> stars = new ArrayList<>();
    //private Set<Planet> planets = Sets.newHashSet();
    //private Set<IChildBody> childs = Sets.newHashSet();

    public static final Codec<StarSystem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("system_name").forGetter(StarSystem::getName),
            ResourceLocation.CODEC.listOf().fieldOf("stars").forGetter(StarSystem::getStarReferences)
    ).apply(instance, StarSystem::new));

    public StarSystem(String name) {
        this.systemName = name;
    }

    public StarSystem(String name, List<ResourceLocation> starReferences)
    {
        this.systemName = name;
        this.starReferences = starReferences;
    }

    public void addStar(Star star) {
        this.stars.forEach(s -> {
            if(s.getName().equals(star.getName()))
                throw new RuntimeException(String.format("Star %s already registered!", star.getName()));
        });

        this.stars.add(star);
    }

    public List<Star> getStars() {
        return ImmutableList.copyOf(stars);
    }

    public Star getMainStar() { return getStars().get(0);}

    public String getName() {
        return this.systemName;
    }

    public List<ResourceLocation> getStarReferences() {
        return Collections.unmodifiableList(starReferences);
    }

    public void resolveStars(Map<ResourceLocation, Star> starMap) {
        for (ResourceLocation starReference : starReferences) {
            Star star = starMap.get(starReference);
            if (star != null) {
                star.setParentStarSystem(this);
                stars.add(star);
            } else {
                System.err.println("Star not found: " + starReference);
            }
        }
    }

}
