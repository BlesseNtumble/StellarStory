package stellarstory.client.renderer.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Matrix4f;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.IChildBody;
import stellarstory.api.celestial.basic_type.Planet;

public class StationSky extends CelestialSky {

    public StationSky() {
        super(1.0F);

    }

    @Override
    protected void renderBodySky(ClientLevel level, int ticks, float partialTicks, PoseStack poseStack, Camera camera, Matrix4f matrix) {

        CelestialBody body = CelestialRegisters.getCelestialBodyFromLevel(level);
        if (!(body instanceof IChildBody child)) return;

        Planet parent = child.getParentPlanet();
        if (parent == null || parent.getIcon() == null) return;

        float timeOfDay = level.getTimeOfDay(partialTicks);
        float spinPeriod = body.getBodyDataHandler().physicalData().dayLength(); //24000.0F;
        float spin = ((ticks + partialTicks) % spinPeriod) / spinPeriod * 360.0F;
        float precession = 5F;

        float tilt = 75.0F;
        float size = 70F;
        skyHelper.renderLitCubeFixed(poseStack,
                -90.0F, 0.0F, 100.0F,
                tilt, precession, spin,
                size, 1.0F,
                timeOfDay,
                parent.getIcon());

        skyHelper.renderAtmosphereFixed(poseStack,
                -90.0F, 0.0F, 100.0F,
                tilt, precession, spin,
                size, 1.02F, 4,
                0.3F, 0.6F, 1.0F, 0.95F, 2.0F,
                timeOfDay);
    }
}
