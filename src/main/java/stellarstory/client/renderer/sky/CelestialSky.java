package stellarstory.client.renderer.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.IChildBody;
import stellarstory.api.celestial.basic_type.Planet;
import stellarstory.client.renderer.sky.helper.SkyHelper;
import stellarstory.client.renderer.sky.helper.StarHelper;


import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class CelestialSky extends DimensionSpecialEffects {

    private static final ResourceLocation SUN_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/environment/sun.png");

    //private final VertexBuffer starBuffer = StarHelper.createStars(0.1F, 190, 160, -1);
    private final Minecraft minecraft = Minecraft.getInstance();
    protected StarHelper starHelper = new StarHelper();
    protected SkyHelper skyHelper = new SkyHelper();

    public CelestialSky() {
        super(Float.NaN, false, SkyType.NONE, false, false);

        starHelper.createStars(0.15F, 5000, 5000, 255, 255, 255);
        skyHelper.createLightSky();
        skyHelper.createDarkSky();
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTicks, PoseStack poseStack, Camera camera,
                             Matrix4f matrix, boolean isFoggy, Runnable setupFog) {

        setupFog.run();
        if (!isFoggy) {
            FogType fogtype = camera.getFluidInCamera();
            if (fogtype != FogType.POWDER_SNOW && fogtype != FogType.LAVA
                    && !this.doesMobEffectBlockSky(camera)) {
                CelestialBody body = CelestialRegisters.getCelestialBodyFromPlayer(this.minecraft.player);

                if (body != null) {

                    ResourceLocation icon = null;
                    if(body instanceof Planet planet)
                        icon = planet.getParentStar().orElseThrow().getIcon();
                    else if (body instanceof IChildBody child)
                        icon = child.getParentPlanet().getParentStar().orElseThrow().getIcon();

                    if(icon != null) {
                        float sunBrightness = 1.0F;
                        float starBrightness = minecraft.level.getStarBrightness(partialTicks) * sunBrightness + 0.5F;
                        skyHelper.renderSky(starHelper.getStarBuffer(), matrix, poseStack, minecraft, icon, partialTicks, sunBrightness, starBrightness, setupFog);
                    }
                }
            }
        }
        return true;
    }

    @Nullable
    @Override
    public float[] getSunriseColor(float p_108872_, float p_108873_) {
        return null;
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return false;
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return Vec3.ZERO;
        // return color.multiply((double) (brightness * 0.94F + 0.06F), (double) (brightness * 0.94F + 0.06F), (double) (brightness * 0.91F + 0.09F));
    }

    /**
     * Бесполезный мусор?, неправильно переданы аргументы. Вместо z - y
     */
    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    private boolean doesMobEffectBlockSky(Camera camera) {
        Entity entity = camera.getEntity();
        if (!(entity instanceof LivingEntity livingentity)) {
            return false;
        } else {
            return livingentity.hasEffect(MobEffects.BLINDNESS) || livingentity.hasEffect(MobEffects.DARKNESS);
        }
    }
}
