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
    private List<ResourceLocation> barycenterReferences = new ArrayList<>();
    private List<Star> stars = new ArrayList<>();
    private List<Barycenter> barycenters = new ArrayList<>();

    // Position in galactic space (used for map rendering)
    public double worldX = 0;
    public double worldY = 0;
    public double worldZ = 0;

    public static final Codec<StarSystem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("system_name").forGetter(StarSystem::getName),
            ResourceLocation.CODEC.listOf().fieldOf("stars").forGetter(StarSystem::getStarReferences),
            ResourceLocation.CODEC.listOf().fieldOf("barycenters").orElse(List.of()).forGetter(StarSystem::getBarycenterReferences),
            Codec.DOUBLE.fieldOf("position_x").orElse(0.0).forGetter(s -> s.worldX),
            Codec.DOUBLE.fieldOf("position_y").orElse(0.0).forGetter(s -> s.worldY),
            Codec.DOUBLE.fieldOf("position_z").orElse(0.0).forGetter(s -> s.worldZ)
    ).apply(instance, StarSystem::create));

    public static StarSystem create(String name, List<ResourceLocation> stars, List<ResourceLocation> barycenters,
                                double wx, double wy, double wz) {
        StarSystem sys = new StarSystem(name, stars);
        sys.barycenterReferences = barycenters;
        sys.worldX = wx;
        sys.worldY = wy;
        sys.worldZ = wz;
        return sys;
    }

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

    public List<ResourceLocation> getBarycenterReferences() {
        return Collections.unmodifiableList(barycenterReferences);
    }

    public List<Barycenter> getBarycenters() {
        return ImmutableList.copyOf(barycenters);
    }

    public void resolveBarycenters(Map<ResourceLocation, Barycenter> barycenterMap) {
        for (ResourceLocation reference : barycenterReferences) {
            Barycenter barycenter = barycenterMap.get(reference);
            if (barycenter != null) {
                barycenters.add(barycenter);
            } else {
                System.err.println("Barycenter not found: " + reference);
            }
        }
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

    public double getWorldX() { return worldX; }
    public double getWorldY() { return worldY; }
    public double getWorldZ() { return worldZ; }
    public void setWorldPosition(double x, double y, double z) {
        this.worldX = x;
        this.worldY = y;
        this.worldZ = z;
    }
}
