package stellarstory.client.renderer.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.IChildBody;
import stellarstory.api.celestial.basic_type.Planet;
import stellarstory.client.renderer.sky.helper.SkyHelper;
import stellarstory.api.celestial.data.OrbitData;

public class MoonSky extends CelestialSky {

    private Minecraft minecraft = Minecraft.getInstance();

    public MoonSky() {
        super(1.0F);
    }

    @Override
    protected void renderBodySky(ClientLevel level, int ticks, float partialTicks, PoseStack poseStack, Camera camera, Matrix4f matrix) {

        CelestialBody body = CelestialRegisters.getCelestialBodyFromLevel(level);
        if (!(body instanceof IChildBody child)) return;

        Planet parent = child.getParentPlanet();
        if (parent == null || parent.getIcon() == null) return;

        float timeOfDay = level.getTimeOfDay(partialTicks);
        float spinPeriod = (float) body.getBodyDataHandler().physicalData().dayLength();
        float spinDeg = ((ticks + partialTicks) % spinPeriod) / spinPeriod * 360.0F;

        // Орбита: скорость в оборотах за игровой день
        OrbitData orbit = parent.getOrbit();
        float orbitSpeed = 0;//24000.0F / orbit.period();
        // phaseOffset в долях суток = фаза (радианы) / 2π
        float orbitPhaseOffset = orbit.phase() / (float) (Math.PI * 2.0);
        float orbitInclination = orbit.inclination();
        float orbitDistance = orbit.semiMajorAxis();

        float playerX = (float) minecraft.player.position().x;
        float playerZ = (float) minecraft.player.position().z;
        float azimuthDegX = (float) (((playerX % 1200) + 1200.0) % 1200.0 / 1200.0 * 360.0);
        float azimuthDegZ = (float) (((playerZ % 1200) + 1200.0) % 1200.0 / 1200.0 * 360.0);


        SkyHelper.BodyParams moon = SkyHelper.BodyParams.fixed(azimuthDegX, azimuthDegZ, 100F)/*.orbit(orbitDistance, orbitSpeed, orbitPhaseOffset, orbitInclination)*/
                .tilt(5.0F, 0.0F).spin(spinDeg);

        float size = 15.0F;
        // Универсальный рендер: освещение синхронизировано с положением тела
        skyHelper.renderLitBody(poseStack, moon, size, 1.0F, timeOfDay, null, parent.getIcon());

        skyHelper.renderAtmosphereBody(poseStack, moon, size, 1.02F, 4,
                0.3F, 0.6F, 1.0F, 0.95F, 2.0F, timeOfDay, null);
    }

}
