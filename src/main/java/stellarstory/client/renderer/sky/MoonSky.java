package stellarstory.client.renderer.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import stellarstory.StellarStory;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;

public class MoonSky extends CelestialSky {

    private static final ResourceLocation CUBE_TEXTURE = ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, "textures/celestialbodies/earth.png");
    private static final CelestialBody OVERWORLD = CelestialRegisters.getCelestialBody("overworld");

    public MoonSky() {
        super();
        skyHelper.createCube(1.0F);
    }

    @Override
    protected void renderBodySky(ClientLevel level, int ticks, float partialTicks, PoseStack poseStack, Camera camera, Matrix4f matrix) {

        if(OVERWORLD == null) return;

        // Axial tilt + slow self-rotation
        float earthRotY = ((ticks + partialTicks) % 24000) / 24000.0F * 360.0F;
        float rotX = 45.0F;
        float rotZ = 60.0F;
        float timeOfDay = level.getTimeOfDay(partialTicks);
        float elevationDeg = -40.0F;
        float azimuthDeg = 50.0F;
        float orbitPhaseOffset = 0.1F; // opposite the sun
        float orbitInclination = 25.0F; // tilt orbit path 25° from vertical

        skyHelper.renderLitCube(poseStack,
                0.0F, 100.0F, 0.0F,
                rotX, earthRotY, rotZ, 15.0F, 1.0F,
                timeOfDay, elevationDeg, azimuthDeg,
                orbitPhaseOffset, orbitInclination,
                OVERWORLD.getIcon());

        // Atmosphere glow (rendered after the cube)
        skyHelper.renderAtmosphere(poseStack,
                0.0F, 100.0F, 0.0F,
                rotX, earthRotY, rotZ,
                15.0F, 1.02F, 4,
                0.3F, 0.6F, 1.0F, 0.95F, 2.0F,
                timeOfDay, elevationDeg, azimuthDeg,
                orbitPhaseOffset, orbitInclination);

       /* skyHelper.renderSkySprite(poseStack,
                NEBULA_TEXTURE,
                40.0F, 100.0F,     // size, distance
                0.5F, 0.7F,        // brightness, alpha
                true, 0.5F,        // orbit=true, phaseOffset=0.5 (opposite the sun)
                0.0F, 0.0F,        // elevation/azimuth ignored when orbit=true
                (ticks + partialTicks) * 0.01F,  // slow self-rotation
                timeOfDay);*/

        /*
        skyHelper.renderSkySprite(poseStack,
        GALAXY_TEXTURE,
        25.0F, 100.0F,     // size, distance
        0.6F, 0.8F,        // brightness, alpha
        false, 0.0F,       // orbit=false, phaseOffset ignored
        -30.0F, 120.0F,    // elevation, azimuth
        0.0F,              // selfRotation
        timeOfDay);
         */
    }

}
