package stellarstory.client.renderer.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
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
public abstract class CelestialSky extends DimensionSpecialEffects {

    private static final ResourceLocation SUN_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/environment/sun.png");


    protected final Minecraft minecraft = Minecraft.getInstance();
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
                        skyHelper.renderSky(starHelper.getStarBuffer(), matrix, poseStack, minecraft, icon, partialTicks, sunBrightness, starBrightness, 0.0F, setupFog);

                        float timeOfDay = level.getTimeOfDay(partialTicks);
                        float orbitPhaseOffset = 0.0F;
                        float orbitInclination = 0.0F;
                        skyHelper.renderAura(poseStack,
                                0.0F, 100.0F, 0.0F,
                                22.0F,                          // auraSize (больше scale куба = 15)
                                1.0F, 1.0F, 1.0F,              // r, g, b (тёплый звёздный свет)
                                1.5F, 0.6F, 4,                  // brightness, alpha, layers
                                timeOfDay,
                                orbitPhaseOffset, orbitInclination);

                        skyHelper.renderCube(poseStack, 0.0F, 100.0F, 0.0F, 50.0F, 15.0F, 0.0F, 5.0F, 1.0F, timeOfDay, orbitPhaseOffset, orbitInclination, icon);

                    }

                    this.renderBodySky(level, ticks, partialTicks, poseStack, camera, matrix);

//                        // Orbit across the sky like the sun
//                        poseStack.pushPose();
//                        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
//                        poseStack.mulPose(Axis.XP.rotationDegrees((minecraft.level.getTimeOfDay(partialTicks) * 360.0F) / 2F));
//                        // Slow self-rotation on top of the orbit
//                        float cubeRotY = (ticks + partialTicks) * 0.1F;
//                        float cubeRotX = cubeRotY * 0.5F;
//                        skyHelper.renderCube(poseStack, 0.0F, 100.0F, 0.0F, cubeRotX, cubeRotY, 0.0F, 15.0F, 1.0F, CUBE_TEXTURE);
//                        poseStack.popPose();

                }
            }
        }
        return true;
    }

    protected abstract void renderBodySky(ClientLevel level, int ticks, float partialTicks, PoseStack poseStack,
                                          Camera camera, Matrix4f matrix);

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
