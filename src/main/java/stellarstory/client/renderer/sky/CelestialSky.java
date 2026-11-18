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
import stellarstory.api.celestial.basic_type.Star;
import stellarstory.client.renderer.sky.helper.SkyHelper;
import stellarstory.client.renderer.sky.helper.StarHelper;
import stellarstory.client.utils.Utils;


import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class CelestialSky extends DimensionSpecialEffects {

    protected final Minecraft minecraft = Minecraft.getInstance();
    protected StarHelper starHelper = new StarHelper();
    protected SkyHelper skyHelper = new SkyHelper();

    public CelestialSky(float mainStarSize) {
        super(Float.NaN, false, SkyType.NONE, false, false);

        starHelper.createStars(0.15F, 5000, 5000, 255, 255, 255);
        skyHelper.createLightSky();
        skyHelper.createDarkSky();
        skyHelper.createCube(mainStarSize);
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTicks, PoseStack poseStack, Camera camera,
                             Matrix4f matrix, boolean isFoggy, Runnable setupFog) {

        setupFog.run();
        if (!isFoggy) {
            FogType fogtype = camera.getFluidInCamera();
            if (fogtype != FogType.POWDER_SNOW && fogtype != FogType.LAVA
                    && !this.doesMobEffectBlockSky(camera)) {
                CelestialBody body = CelestialRegisters.getCelestialBodyFromLevel(level);

                if (body != null && !body.isEmpty()) {


                    Star star = null;

                    if(body instanceof Planet)
                        star = ((Planet) body).getParentStar().orElse(null);

                    if(body instanceof IChildBody)
                        star = ((IChildBody) body).getParentPlanet().getParentStar().orElse(null);


                    if (star != null) {
                        float sunBrightness = 1.0F;
                        float starBrightness = minecraft.level.getStarBrightness(partialTicks) * sunBrightness + 0.5F;
                        float starColorR = star.getSpectralClass().r;
                        float starColorG = star.getSpectralClass().g;
                        float starColorB = star.getSpectralClass().b;

                        double safeDistance = Math.max(body.getOrbit().semiMajorAxis(), 0.5);
                        float scale = 1.0F;
                        float coreSize = (30 / (float) safeDistance) * scale;
                        float auraMultiplier = 2.2F + (0.5F / (float) safeDistance);
                        float auraSize = coreSize * auraMultiplier;
                        float baseRayLen = coreSize * 2.5F;

                        float time = Minecraft.getInstance().level.getGameTime() * 0.05F; // Медленное изменение
                        float[] lengths = new float[]{
                                baseRayLen * (2.5F + Mth.sin(time) * 0.3F),       // Луч 1: пульсирует
                                baseRayLen * (3.0F + Mth.cos(time * 0.7F) * 0.4F),// Луч 2: пульсирует с другой фазой
                                baseRayLen * 2.2F,                                // Луч 3: статичный, покороче
                                baseRayLen * 2.8F                                 // Луч 4: статичный, подлиннее
                        };

                        float proximityFactor = (float) Math.min(1.0, 2.0 / safeDistance); // От 0 до 1
                        float rayWidth = 5.05F + (proximityFactor * 0.15F); // От 5.05 до 5.20
                        float rayAlpha = 0.4F + (proximityFactor * 0.5F);


                        float timeOfDay = level.getTimeOfDay(partialTicks);
                        float orbitPhaseOffset = 0.0F;
                        float orbitInclination = 0.0F;


                        skyHelper.renderSky(starHelper.getStarBuffer(), matrix, poseStack, minecraft, partialTicks, sunBrightness, starBrightness, 0.0F, setupFog);

                        skyHelper.renderAura(poseStack,
                                0.0F, 100.0F, 0.0F,
                                auraSize,                          // auraSize (больше scale куба = 15)
                                starColorR, starColorG, starColorB,              // r, g, b (тёплый звёздный свет)
                                1.0F, 0.6F, 4,                  // brightness, alpha, layers
                                timeOfDay,
                                orbitPhaseOffset, orbitInclination);

                        skyHelper.renderStarRays(
                                poseStack,
                                0.0F, 100.0F, 0.0F,
                                starColorR, starColorG, starColorB, // Твои float цвета (например, из enum)
                                lengths,
                                rayWidth,  // baseWidth: толщина луча у основания (подбери под размер звезды)
                                rayAlpha,   // maxAlpha: непрозрачность у центра (не ставь 1.0F, иначе будет резкий переход)
                                time * 0.01F, // rotationOffset: медленное вращение всего креста лучей
                                timeOfDay,
                                orbitPhaseOffset, orbitInclination
                        );

                        skyHelper.renderCube(poseStack, 0.0F, 100.0F, 0.0F, 50.0F, 15.0F, 0.0F, coreSize, timeOfDay, orbitPhaseOffset, orbitInclination, starColorR, starColorG, starColorB, 1.0F, star.getIcon());

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
