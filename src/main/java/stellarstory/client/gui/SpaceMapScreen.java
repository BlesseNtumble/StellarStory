package stellarstory.client.gui;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.Barycenter;
import stellarstory.api.celestial.StarSystem;
import stellarstory.api.celestial.basic_type.Asteroid;
import stellarstory.api.celestial.basic_type.Comet;
import stellarstory.api.celestial.basic_type.Moon;
import stellarstory.api.celestial.basic_type.Planet;
import stellarstory.api.celestial.basic_type.Star;
import stellarstory.api.celestial.data.OrbitData;
import stellarstory.api.celestial.data.OrbitVisual;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Stellar map — celestial bodies are rendered as 3D cubes that spin around
 * their own axes and are shaded with a Lambertian model from their parent star.
 * Orbits are projected to screen space and drawn via {@link GuiGraphics}.
 * Supports multiple star systems with their own galactic positions.
 */
@OnlyIn(Dist.CLIENT)
public class SpaceMapScreen extends Screen {

    /** Identity matrix used to pass raw screen-space coords to BufferBuilder.vertex. */
    private static final Matrix4f CUBE_IDENTITY = new Matrix4f();

    // ── Camera state ──────────────────────────────────────────────────────
    private float camRotX = 25.0F;          // elevation (pitch) in degrees
    private float camRotY = -45.0F;          // azimuth (yaw) in degrees
    private float zoom = 1.0F;               // zoom factor (1.0 = default)

    // ── Camera offset (for centering on selected body) ────────────────────
    private float camOffsetX = 0.0F;
    private float camOffsetY = 0.0F;
    private float camOffsetZ = 0.0F;

    // ── Camera follow mode ────────────────────────────────────────────────
    private boolean followSelected = false;

    // ── Selected body ─────────────────────────────────────────────────────
    @Nullable
    private SelectedBody selectedBody = null;

    /** A body that has been selected by the player. */
    private static class SelectedBody {
        final String name;
        final String type;          // "star", "planet", "moon", "asteroid", "comet"
        final double worldX, worldY, worldZ;  // current world position

        SelectedBody(String name, String type, double wx, double wy, double wz) {
            this.name = name;
            this.type = type;
            this.worldX = wx;
            this.worldY = wy;
            this.worldZ = wz;
        }
    }

    // ── Mouse interaction ─────────────────────────────────────────────────
    private boolean isPanning = false;
    private boolean isRotating = false;
    private double panStartX, panStartY;
    private double rotStartX, rotStartY;
    private float panOffsetX, panOffsetY;

    // ── Orbit animation ───────────────────────────────────────────────────
    private float timeOfDay = 0.0F;

    // ── Cached body data ──────────────────────────────────────────────────
    private static class BodyRenderInfo {
        final String name;
        final String type;          // "planet", "moon", "asteroid", "comet"
        final OrbitData orbit;
        final OrbitVisual orbitVisual;
        final float spinSpeed;      // body rotation speed (radians per tick unit)
        final float bodySize;
        final float r, g, b;
        final net.minecraft.resources.ResourceLocation icon;  // texture path
        final List<BodyRenderInfo> children = new ArrayList<>();

        // ── Asteroid orbit scatter (only for type "asteroid") ────────────
        // Small rocks spread along the entire orbit; the body itself (bodySize)
        // is the one large selectable rock at the current orbit position.
        final int rockCount;
        final float[] rockPhase;        // phase offset along the orbit (radians)
        final float[] rockRadiusScale;  // radial scatter (0.85..1.15)
        final float[] rockYOff;         // vertical scatter
        final float[] rockSize;

        BodyRenderInfo(String name, String type, OrbitData orbit, OrbitVisual orbitVisual,
                       float spinSpeed, float bodySize, float r, float g, float b,
                       net.minecraft.resources.ResourceLocation icon) {
            this.name = name;
            this.type = type;
            this.orbit = orbit;
            this.orbitVisual = orbitVisual;
            this.spinSpeed = spinSpeed;
            this.bodySize = bodySize;
            this.r = r;
            this.g = g;
            this.b = b;
            this.icon = icon;

            if ("asteroid".equals(type)) {
                Random rng = new Random(name.hashCode() * 31L + 7L);
                rockCount = 14 + rng.nextInt(8);   // 14..21 small rocks along the orbit
                rockPhase = new float[rockCount];
                rockRadiusScale = new float[rockCount];
                rockYOff = new float[rockCount];
                rockSize = new float[rockCount];

                // Belt width configurable via OrbitVisual (spread = radial, thickness = vertical)
                float spread = orbitVisual.spread();
                float thickness = orbitVisual.thickness();
                float baseRadius = Math.max(0.5F, orbit.semiMajorAxis());
                for (int i = 0; i < rockCount; i++) {
                    rockPhase[i] = (float)i / rockCount * Mth.TWO_PI + (rng.nextFloat() - 0.5F) * 0.3F;
                    // Radial scatter: 1.0 +- spread (as a fraction of the base radius), clamped
                    // so the rock never collapses onto the star even with large spread.
                    rockRadiusScale[i] = Mth.clamp(
                            1.0F + (rng.nextFloat() * 2.0F - 1.0F) * (spread / baseRadius),
                            0.2F, 3.0F);
                    // Vertical scatter: +- thickness in map units
                    rockYOff[i] = (rng.nextFloat() * 2.0F - 1.0F) * thickness;
                    rockSize[i] = 0.4F + rng.nextFloat() * 0.5F;
                }
            } else {
                rockCount = 0;
                rockPhase = new float[0];
                rockRadiusScale = new float[0];
                rockYOff = new float[0];
                rockSize = new float[0];
            }
        }
    }

    private static class StarRenderInfo {
        final String name;
        final float r, g, b;
        final float size;
        final OrbitData orbit;         // used only when barycenter != null
        final OrbitVisual orbitVisual; // visual orbit parameters
        final Barycenter barycenter;   // nullable — orbit centre override
        final float spinSpeed;         // star rotation speed
        final net.minecraft.resources.ResourceLocation icon;  // texture path
        final List<BodyRenderInfo> planets = new ArrayList<>();
        final List<BodyRenderInfo> asteroids = new ArrayList<>();
        final List<BodyRenderInfo> comets = new ArrayList<>();

        StarRenderInfo(String name, float r, float g, float b, float size,
                       OrbitData orbit, OrbitVisual orbitVisual, Barycenter barycenter,
                       float spinSpeed, net.minecraft.resources.ResourceLocation icon) {
            this.name = name;
            this.r = r;
            this.g = g;
            this.b = b;
            this.size = size;
            this.orbit = orbit;
            this.orbitVisual = orbitVisual;
            this.barycenter = barycenter;
            this.spinSpeed = spinSpeed;
            this.icon = icon;
        }
    }

    /** A star system with its galactic position. */
    private static class SystemRenderInfo {
        final String name;
        final double worldX, worldY, worldZ;   // galactic coordinates
        final List<Barycenter> barycenters = new ArrayList<>();
        final List<StarRenderInfo> stars = new ArrayList<>();

        SystemRenderInfo(String name, double wx, double wy, double wz) {
            this.name = name;
            this.worldX = wx;
            this.worldY = wy;
            this.worldZ = wz;
        }
    }

    private final List<SystemRenderInfo> systems = new ArrayList<>();

    // ── Catalog ───────────────────────────────────────────────────────────
    private static class CatalogEntry {
        final String name;
        final int color;          // ARGB, alpha=255
        final String type;        // "star", "planet", "moon"

        CatalogEntry(String name, int color, String type) {
            this.name = name;
            this.color = color;
            this.type = type;
        }
    }

    private final List<CatalogEntry> catalog = new ArrayList<>();
    private static final int CATALOG_X = 8;
    private static final int CATALOG_WIDTH = 140;
    private static final int CATALOG_ITEM_HEIGHT = 18;
    private static final int CATALOG_HEADER_HEIGHT = 20;
    private static final int CATALOG_MAX_VISIBLE = 12;

    @Nullable
    private String hoveredBodyName = null;

    // ──────────────────────────────────────────────────────────────────────
    public SpaceMapScreen() {
        super(Component.translatable("gui.stellarstory.space_map"));
        buildRenderData();
        buildCatalog();
    }

    /** Build the catalog list from all celestial bodies. */
    private void buildCatalog() {
        catalog.clear();

        for (SystemRenderInfo sys : systems) {
            for (StarRenderInfo star : sys.stars) {
                // Star color
                int starColor = 0xFF000000
                        | ((int)(star.r * 255) << 16)
                        | ((int)(star.g * 255) << 8)
                        | (int)(star.b * 255);
                catalog.add(new CatalogEntry(star.name, starColor, "star"));

                for (BodyRenderInfo planet : star.planets) {
                    int planetColor = 0xFF000000
                            | ((int)(planet.r * 255) << 16)
                            | ((int)(planet.g * 255) << 8)
                            | (int)(planet.b * 255);
                    catalog.add(new CatalogEntry(planet.name, planetColor, "planet"));

                    for (BodyRenderInfo child : planet.children) {
                        int childColor = 0xFF000000
                                | ((int)(child.r * 255) << 16)
                                | ((int)(child.g * 255) << 8)
                                | (int)(child.b * 255);
                        catalog.add(new CatalogEntry(child.name, childColor, child.type));
                    }
                }

                for (BodyRenderInfo asteroid : star.asteroids) {
                    int aColor = 0xFF000000
                            | ((int)(asteroid.r * 255) << 16)
                            | ((int)(asteroid.g * 255) << 8)
                            | (int)(asteroid.b * 255);
                    catalog.add(new CatalogEntry(asteroid.name, aColor, asteroid.type));
                }

                for (BodyRenderInfo comet : star.comets) {
                    int cColor = 0xFF000000
                            | ((int)(comet.r * 255) << 16)
                            | ((int)(comet.g * 255) << 8)
                            | (int)(comet.b * 255);
                    catalog.add(new CatalogEntry(comet.name, cColor, comet.type));
                }
            }
        }
    }

    private void buildRenderData() {
        systems.clear();

        for (Map.Entry<ResourceLocation, StarSystem> entry : CelestialRegisters.getStarSystems().entrySet()) {
            StarSystem system = entry.getValue();
            // Use galactic position from system, default to 0,0,0
            double wx = system.getWorldX();
            double wy = system.getWorldY();
            double wz = system.getWorldZ();
            SystemRenderInfo sysInfo = new SystemRenderInfo(system.getName(), wx, wy, wz);
            sysInfo.barycenters.addAll(system.getBarycenters());

            int starCount = system.getStars().size();
            for (int si = 0; si < starCount; si++) {
                Star star = system.getStars().get(si);
                Barycenter barycenter = star.getBarycenter().orElse(null);

                StarRenderInfo starInfo = new StarRenderInfo(
                        star.getName(),
                        1.0F, 0.85F + (si * 0.07F) % 0.15F, 0.5F + (si * 0.11F) % 0.3F,
                        8.0F,
                        star.getOrbit(),
                        star.getOrbitVisual(),
                        barycenter,
                        0.05F + si * 0.02F,
                        star.getIcon()
                );

                // Planets
                List<Planet> planets = star.getPlanets();
                int planetCount = planets.size();
                for (int pi = 0; pi < planetCount; pi++) {
                    Planet planet = planets.get(pi);

                    float pr = 0.3F + ((planet.hashCode() & 0xFF) % 100) / 100.0F * 0.7F;
                    float pg = 0.3F + ((planet.hashCode() >> 8 & 0xFF) % 100) / 100.0F * 0.7F;
                    float pb = 0.3F + ((planet.hashCode() >> 16 & 0xFF) % 100) / 100.0F * 0.7F;

                    BodyRenderInfo planetInfo = new BodyRenderInfo(
                            planet.getName(), "planet", planet.getOrbit(), planet.getOrbitVisual(),
                            0.9F + pi * 0.2F, 5.0F, pr, pg, pb,
                            planet.getIcon()
                    );

                    // Moons
                    for (var child : planet.getChilds()) {
                        if (child instanceof Moon moon) {
                            float mr = 0.5F + ((moon.hashCode() & 0xFF) % 60) / 100.0F * 0.3F;
                            float mg = 0.5F + ((moon.hashCode() >> 8 & 0xFF) % 60) / 100.0F * 0.3F;
                            float mb = 0.5F + ((moon.hashCode() >> 16 & 0xFF) % 60) / 100.0F * 0.3F;

                            planetInfo.children.add(new BodyRenderInfo(
                                    moon.getName(), "moon", moon.getOrbit(), moon.getOrbitVisual(),
                                    0.5F + ((moon.hashCode() >> 4 & 0xF)) / 10.0F, 2.0F, mr, mg, mb,
                                    moon.getIcon()
                            ));
                        }
                    }

                    // Asteroids orbiting the planet (Phobos/Deimos style)
                    for (Asteroid asteroid : planet.getAsteroids()) {
                        float ar = 0.45F + ((asteroid.hashCode() & 0xFF) % 60) / 100.0F * 0.3F;
                        float ag = 0.42F + ((asteroid.hashCode() >> 8 & 0xFF) % 60) / 100.0F * 0.3F;
                        float ab = 0.38F + ((asteroid.hashCode() >> 16 & 0xFF) % 60) / 100.0F * 0.3F;

                        planetInfo.children.add(new BodyRenderInfo(
                                asteroid.getName(), "asteroid", asteroid.getOrbit(), asteroid.getOrbitVisual(),
                                1.5F + ((asteroid.hashCode() >> 6 & 0xF)) / 5.0F, 2.6F, ar, ag, ab,
                                asteroid.getIcon()
                        ));
                    }

                    starInfo.planets.add(planetInfo);
                }

                // Asteroids orbiting the star directly
                for (Asteroid asteroid : star.getAsteroids()) {
                    float ar = 0.45F + ((asteroid.hashCode() & 0xFF) % 60) / 100.0F * 0.3F;
                    float ag = 0.42F + ((asteroid.hashCode() >> 8 & 0xFF) % 60) / 100.0F * 0.3F;
                    float ab = 0.38F + ((asteroid.hashCode() >> 16 & 0xFF) % 60) / 100.0F * 0.3F;

                    starInfo.asteroids.add(new BodyRenderInfo(
                            asteroid.getName(), "asteroid", asteroid.getOrbit(), asteroid.getOrbitVisual(),
                            1.5F + ((asteroid.hashCode() >> 6 & 0xF)) / 5.0F, 2.8F, ar, ag, ab,
                            asteroid.getIcon()
                    ));
                }

                // Comets
                for (Comet comet : star.getComets()) {
                    starInfo.comets.add(new BodyRenderInfo(
                            comet.getName(), "comet", comet.getOrbit(), comet.getOrbitVisual(),
                            1.0F, 2.2F, 0.7F, 0.85F, 1.0F,
                            comet.getIcon()
                    ));
                }

                sysInfo.stars.add(starInfo);
            }
            systems.add(sysInfo);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Update orbit time
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            this.timeOfDay = level.getTimeOfDay(partialTick);
        }

        this.renderBackground(guiGraphics);

        // ── Camera follow selected body ────────────────────────────────────
        if (selectedBody != null && followSelected) {
            // Compute current world position of the selected body
            float[] pos = findBodyWorldPos(selectedBody.name);
            if (pos != null) {
                // Smoothly interpolate camOffset toward the body's current position
                float lerpSpeed = 0.08F;
                camOffsetX += (pos[0] - camOffsetX) * lerpSpeed;
                camOffsetY += (pos[1] - camOffsetY) * lerpSpeed;
                camOffsetZ += (pos[2] - camOffsetZ) * lerpSpeed;
            }
        }

        // Dark background — fullscreen
        guiGraphics.fill(0, 0, width, height, 0xFF0A0A1A);

        // half is unused by toScreen now (it uses width/height directly)
        float half = 0;

        // ── 2D pass: orbit rings + star glows ──────────────────────────
        List<CubeRenderData> cubes = new ArrayList<>();

        for (SystemRenderInfo sys : systems) {
            for (StarRenderInfo star : sys.stars) {
                // Orbit ring for a star orbiting a barycenter
                if (star.barycenter != null) {
                    float bcx = (float)sys.worldX + (float)star.barycenter.getX();
                    float bcy = (float)sys.worldY + (float)star.barycenter.getY();
                    float bcz = (float)sys.worldZ + (float)star.barycenter.getZ();
                    drawOrbitRing(guiGraphics, bcx, bcy, bcz, star.orbit, star.orbitVisual, rgba(0.6F, 0.5F, 0.3F, 0.25F));
                }

                float[] starW = starWorldPos(sys, star);
                float starWX = starW[0], starWY = starW[1], starWZ = starW[2];

                // Glow layers (2D, drawn in this pass)
                renderStarGlow(guiGraphics, star, starWX, starWY, starWZ, half);

                // Star cube is emissive — it is its own light source
                cubes.add(new CubeRenderData(starWX, starWY, starWZ, star.size,
                        star.r, star.g, star.b, 0.0F, 0.0F, 0.0F, 0.5F,
                        true, star.icon));

                // ── Planets with Lambertian lighting from their star ─────
                for (BodyRenderInfo planet : star.planets) {
                    float[] pw = bodyWorldPos(starWX, starWY, starWZ, planet);
                    float planetWX = pw[0], planetWY = pw[1], planetWZ = pw[2];

                    drawOrbitRing(guiGraphics, starWX, starWY, starWZ, planet.orbit, planet.orbitVisual, rgba(0.3F, 0.4F, 0.6F, 0.5F));

                    // Direction from planet toward the star (normalized)
                    float[] light = dirTo(planetWX, planetWY, planetWZ, starWX, starWY, starWZ);

                    cubes.add(new CubeRenderData(planetWX, planetWY, planetWZ, planet.bodySize,
                            planet.r, planet.g, planet.b, light[0], light[1], light[2], 0.3F,
                            false, planet.icon));

                    // ── Moons & planet-asteroids orbiting the planet ─────
                    for (BodyRenderInfo child : planet.children) {
                        float[] cw = bodyWorldPos(planetWX, planetWY, planetWZ, child);

                        if ("asteroid".equals(child.type)) {
                            // Small rocks spread along the child's orbit
                            for (int ri = 0; ri < child.rockCount; ri++) {
                                float[] ro = orbitOffset(child.orbit, this.timeOfDay,
                                        child.rockPhase[ri], child.rockRadiusScale[ri], child.rockYOff[ri]);
                                float rx = planetWX + ro[0];
                                float ry = planetWY + ro[1];
                                float rz = planetWZ + ro[2];
                                float[] cl = dirTo(rx, ry, rz, starWX, starWY, starWZ);
                                cubes.add(new CubeRenderData(rx, ry, rz, child.rockSize[ri],
                                        child.r, child.g, child.b, cl[0], cl[1], cl[2], 0.3F,
                                        false, child.icon));
                            }
                            // The selectable asteroid itself — the one large rock
                            float[] cl2 = dirTo(cw[0], cw[1], cw[2], starWX, starWY, starWZ);
                            cubes.add(new CubeRenderData(cw[0], cw[1], cw[2], child.bodySize,
                                    child.r, child.g, child.b, cl2[0], cl2[1], cl2[2], 0.3F,
                                    false, child.icon));
                        } else {
                            drawOrbitRing(guiGraphics, planetWX, planetWY, planetWZ, child.orbit, child.orbitVisual,
                                    rgba(0.5F, 0.5F, 0.5F, 0.15F));

                            // Direction from the child toward the star (not the parent planet)
                            float[] cl = dirTo(cw[0], cw[1], cw[2], starWX, starWY, starWZ);

                            cubes.add(new CubeRenderData(cw[0], cw[1], cw[2], child.bodySize,
                                    child.r, child.g, child.b, cl[0], cl[1], cl[2], 0.2F,
                                    false, child.icon));
                        }
                    }
                }

                // ── Asteroids: small rocks along the orbit + one large selectable rock ──
                for (BodyRenderInfo asteroid : star.asteroids) {
                    float[] aw = bodyWorldPos(starWX, starWY, starWZ, asteroid);

                    // Small rocks spread along the whole orbit
                    for (int ri = 0; ri < asteroid.rockCount; ri++) {
                        float[] ro = orbitOffset(asteroid.orbit, this.timeOfDay,
                                asteroid.rockPhase[ri], asteroid.rockRadiusScale[ri], asteroid.rockYOff[ri]);
                        float rx = starWX + ro[0];
                        float ry = starWY + ro[1];
                        float rz = starWZ + ro[2];
                        float[] al = dirTo(rx, ry, rz, starWX, starWY, starWZ);
                        cubes.add(new CubeRenderData(rx, ry, rz, asteroid.rockSize[ri],
                                asteroid.r, asteroid.g, asteroid.b, al[0], al[1], al[2], 0.3F,
                                false, asteroid.icon));
                    }

                    // The selectable asteroid itself — the one large rock
                    float[] al2 = dirTo(aw[0], aw[1], aw[2], starWX, starWY, starWZ);
                    cubes.add(new CubeRenderData(aw[0], aw[1], aw[2], asteroid.bodySize,
                            asteroid.r, asteroid.g, asteroid.b, al2[0], al2[1], al2[2], 0.3F,
                            false, asteroid.icon));
                }

                // ── Comets: elongated orbit + tail ──────────────────────
                for (BodyRenderInfo comet : star.comets) {
                    float[] cw = bodyWorldPos(starWX, starWY, starWZ, comet);

                    drawOrbitRing(guiGraphics, starWX, starWY, starWZ, comet.orbit, comet.orbitVisual,
                            rgba(0.4F, 0.7F, 1.0F, 0.6F));

                    renderCometTail(guiGraphics, starWX, starWY, starWZ, cw[0], cw[1], cw[2]);

                    float[] cl = dirTo(cw[0], cw[1], cw[2], starWX, starWY, starWZ);

                    cubes.add(new CubeRenderData(cw[0], cw[1], cw[2], comet.bodySize,
                            comet.r, comet.g, comet.b, cl[0], cl[1], cl[2], 0.2F,
                            false, comet.icon));
                }
            }
        }

        // ── 3D pass: spinning, star-lit cubes ──────────────────────────
        // Flush the 2D pass so orbit rings & star glows stay behind the cubes,
        // otherwise they would be batched and drawn on top of the 3D geometry.
        guiGraphics.flush();

        // View-space depth range over all face vertices (from projectToScreen).
        // We precompute it so we can map all cubes into the visible [-1000, +1000]
        // Z range for the depth test.
        float minDepth = Float.MAX_VALUE;
        float maxDepth = -Float.MAX_VALUE;
        for (CubeRenderData cube : cubes) {
            float halfSize = Math.max(0.1F, cube.size * 0.5F);

            // Rotation axis (normalized)
            float ax = cube.axisX, ay = cube.axisY, az = cube.axisZ;
            float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            if (len > 0.001F) { ax /= len; ay /= len; az /= len; }
            float spinA = this.timeOfDay * cube.spinSpeed * Mth.TWO_PI * 2.0F * 4.0F;
            float cosA = Mth.cos(spinA), sinA = Mth.sin(spinA);

            // Pre-rotate all 8 vertices
            float[][] rotated = new float[8][3];
            for (int i = 0; i < 8; i++) {
                float vx = -halfSize + (float)(i & 1) * halfSize * 2;
                float vy = -halfSize + (float)((i >> 1) & 1) * halfSize * 2;
                float vz = -halfSize + (float)((i >> 2) & 1) * halfSize * 2;
                float vx_c = ay * vz - az * vy;
                float vy_c = az * vx - ax * vz;
                float vz_c = ax * vy - ay * vx;
                float ax_vc = ay * vz_c - az * vy_c;
                float ay_vc = az * vx_c - ax * vz_c;
                float az_vc = ax * vy_c - ay * vx_c;
                rotated[i][0] = vx + sinA * vx_c + (1.0F - cosA) * ax_vc;
                rotated[i][1] = vy + sinA * vy_c + (1.0F - cosA) * ay_vc;
                rotated[i][2] = vz + sinA * vz_c + (1.0F - cosA) * az_vc;
            }

            for (float[] r : rotated) {
                float[] p = projectToScreen(cube.wx + r[0], cube.wy + r[1], cube.wz + r[2]);
                if (p[2] < minDepth) minDepth = p[2];
                if (p[2] > maxDepth) maxDepth = p[2];
            }
        }
        final float pMin = minDepth;
        final float pMax = maxDepth;
        final float span = Math.max(1.0E-4F, pMax - pMin);

        // Render each cube with its own texture, sharing one depth buffer.
        RenderSystem.clearDepth(1.0F);
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        for (CubeRenderData cube : cubes) {
            // Bind this cube's texture
            RenderSystem.setShaderTexture(0, cube.icon);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            collectCubeFaces(cube, bb, pMin, span);
            BufferUploader.drawWithShader(bb.end());
        }

        // Restore GUI-friendly state: no depth test / writes and a clean depth
        // buffer so the UI overlay is never occluded by cube depth values.
        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.clearDepth(1.0F);
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, false);

        // ── UI overlay ────────────────────────────────────────────────────
        // Catalog on the left
        renderCatalog(guiGraphics, mouseX, mouseY);

        // Title
        String title = Component.translatable("gui.stellarstory.space_map.title").getString();
        guiGraphics.drawString(font, title, (width - font.width(title)) / 2, 8, 0xFFFFFF, false);

        // Hint
        String hint = Component.translatable("gui.stellarstory.space_map.hint").getString();
        guiGraphics.drawString(font, hint, (width - font.width(hint)) / 2, height - 10, 0x666666, false);

        // Selected body info
        if (selectedBody != null) {
            String info = selectedBody.name + " (" + selectedBody.type + ")";
            guiGraphics.drawString(font, info, 10, 8, 0xAAAAFF, false);
        }

        // Hover name
        checkHover(mouseX, mouseY, half);
        if (hoveredBodyName != null) {
            guiGraphics.drawString(font, hoveredBodyName, mouseX - font.width(hoveredBodyName) / 2, mouseY - 20, 0xFFFFFF, true);
        }

        // ── Draw selection indicator ─────────────────────────────────────
        if (selectedBody != null) {
            // Use the body's current (animated) position so the marker follows it
            float[] selPos = findBodyWorldPos(selectedBody.name);
            float sxPos = selPos != null ? selPos[0] : (float)selectedBody.worldX;
            float syPos = selPos != null ? selPos[1] : (float)selectedBody.worldY;
            float szPos = selPos != null ? selPos[2] : (float)selectedBody.worldZ;
            float[] selScreen = toScreen(sxPos, syPos, szPos, half);
            int selR = 12;
            // Selection ring
            guiGraphics.fill((int)selScreen[0] - selR, (int)selScreen[1] - selR,
                    (int)selScreen[0] + selR, (int)selScreen[1] + selR, 0x40FFFFFF);
            guiGraphics.renderOutline((int)selScreen[0] - selR, (int)selScreen[1] - selR,
                    selR * 2, selR * 2, 0xFFFFFF);
        }
    }

    /** Per-frame data for one 3D cube to draw. */
    private static class CubeRenderData {
        final float wx, wy, wz;              // world position
        final float size;                    // cube edge length
        final float r, g, b;                 // base colour
        final float lightX, lightY, lightZ;  // direction toward the light source (normalized)
        final float spinSpeed;               // rotation speed of the cube itself
        final float axisX, axisY, axisZ;     // rotation axis (normalized, default Y)
        final boolean emissive;              // true = unlit (star core)
        final net.minecraft.resources.ResourceLocation icon;  // cube texture

        CubeRenderData(float wx, float wy, float wz, float size, float r, float g, float b,
                       float lightX, float lightY, float lightZ, float spinSpeed, boolean emissive,
                       net.minecraft.resources.ResourceLocation icon) {
            this(wx, wy, wz, size, r, g, b, lightX, lightY, lightZ, spinSpeed,
                 0.0F, 1.0F, 0.0F, emissive, icon);
        }

        CubeRenderData(float wx, float wy, float wz, float size, float r, float g, float b,
                       float lightX, float lightY, float lightZ, float spinSpeed,
                       float axisX, float axisY, float axisZ,
                       boolean emissive, net.minecraft.resources.ResourceLocation icon) {
            this.wx = wx;
            this.wy = wy;
            this.wz = wz;
            this.size = size;
            this.r = r;
            this.g = g;
            this.b = b;
            this.lightX = lightX;
            this.lightY = lightY;
            this.lightZ = lightZ;
            this.spinSpeed = spinSpeed;
            this.axisX = axisX;
            this.axisY = axisY;
            this.axisZ = axisZ;
            this.emissive = emissive;
            this.icon = icon;
        }
    }

    /** Projects (wx,wy,wz) → screen-space offset relative to viewport centre.
     *  The returned vector's z component is the view-space depth (for painter's
     *  algorithm sorting of cube faces). */
    private Vector3f project(float wx, float wy, float wz) {
        // 1. Apply camera offset (selected body position)
        float x = (wx - camOffsetX) * zoom;
        float y = (wy - camOffsetY) * zoom;
        float z = (wz - camOffsetZ) * zoom - 100.0F; // fixed camera distance

        // 2. Rotate X (pitch)
        float cosX = Mth.cos(camRotX * Mth.DEG_TO_RAD);
        float sinX = Mth.sin(camRotX * Mth.DEG_TO_RAD);
        float y1 = y * cosX - z * sinX;
        float z1 = y * sinX + z * cosX;
        y = y1; z = z1;

        // 3. Rotate Y (yaw)
        float cosY = Mth.cos(camRotY * Mth.DEG_TO_RAD);
        float sinY = Mth.sin(camRotY * Mth.DEG_TO_RAD);
        float x1 = x * cosY + z * sinY;
        z = -x * sinY + z * cosY;
        x = x1;

        // 4. Apply pan offset in screen space (after rotation)
        x += panOffsetX;
        y += panOffsetY;

        return new Vector3f(x, y, z);
    }

    /** Converts world coords to screen pixel coords (centered on screen). */
    private float[] toScreen(float wx, float wy, float wz, float half) {
        Vector3f p = project(wx, wy, wz);
        return new float[]{width * 0.5F + p.x, height * 0.5F - p.y};
    }

    /** Projects world coords to screen pixel coords plus the view-space depth
     *  (used for sorting the cube's faces back-to-front). */
    private float[] projectToScreen(float wx, float wy, float wz) {
        Vector3f p = project(wx, wy, wz);
        return new float[]{width * 0.5F + p.x, height * 0.5F - p.y, p.z};
    }

    /** Render a star's glow layers at a world position (the core cube is drawn in the 3D pass). */
    private void renderStarGlow(GuiGraphics guiGraphics, StarRenderInfo star,
                                double worldX, double worldY, double worldZ, float half) {
        float[] s = toScreen((float)worldX, (float)worldY, (float)worldZ, half);
        float sx = s[0], sy = s[1];

        // Glow layers (outer to inner)
        int layers = 3;
        for (int layer = layers - 1; layer >= 0; layer--) {
            float radius = star.size * (0.6F + layer * 0.5F);
            int alpha = (int) (30 * (1.0F - (float) layer / layers));
            int color = (alpha << 24) | ((int)(star.r * 255) << 16) | ((int)(star.g * 255) << 8) | (int)(star.b * 255);
            int r = (int) radius + 1;
            guiGraphics.fill((int)sx - r, (int)sy - r, (int)sx + r, (int)sy + r, color);
        }
    }

    /**
     * Build the 6 quad faces of a body's spinning cube and emit them directly
     * into the given {@code BufferBuilder} using POSITION_TEX_COLOR format.
     * Each vertex carries its own view-space depth (mapped into [-1000, +1000])
     * so the depth test can resolve overlaps.
     *
     * The cube rotates around its own axis (stored in cube.axisX/Y/Z) with speed
     * cube.spinSpeed.  Lambertian shading is applied per-face based on the dot
     * product between the transformed face normal and the light direction.
     */
    private void collectCubeFaces(CubeRenderData cube, BufferBuilder bb,
                                  float pMin, float span) {
        float halfSize = Math.max(0.1F, cube.size * 0.5F);

        // Rotation axis (normalized)
        float ax = cube.axisX, ay = cube.axisY, az = cube.axisZ;
        float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (len > 0.001F) { ax /= len; ay /= len; az /= len; }

        // Spin angle — multiplied by 4 for faster rotation
        float spinA = this.timeOfDay * cube.spinSpeed * Mth.TWO_PI * 2.0F * 4.0F;
        float cosA = Mth.cos(spinA), sinA = Mth.sin(spinA);

        // Local cube vertices (x, y, z), centred on the body position.
        float[][] verts = {
                {-halfSize, -halfSize, -halfSize},
                { halfSize, -halfSize, -halfSize},
                { halfSize, -halfSize,  halfSize},
                {-halfSize, -halfSize,  halfSize},
                {-halfSize,  halfSize, -halfSize},
                { halfSize,  halfSize, -halfSize},
                { halfSize,  halfSize,  halfSize},
                {-halfSize,  halfSize,  halfSize},
        };

        // Faces as vertex index quads + their local normals.
        int[][] faces = {
                {0, 1, 2, 3}, // -Y (bottom)
                {4, 5, 6, 7}, // +Y (top)
                {0, 1, 5, 4}, // -Z (back)
                {1, 2, 6, 5}, // +X (right)
                {2, 3, 7, 6}, // +Z (front)
                {3, 0, 4, 7}, // -X (left)
        };
        float[][] normals = {
                { 0, -1,  0},
                { 0,  1,  0},
                { 0,  0, -1},
                { 1,  0,  0},
                { 0,  0,  1},
                {-1,  0,  0},
        };

        // Pre-compute projected vertices (screen x, y, depth).
        float[][] scr = new float[8][3];
        for (int i = 0; i < 8; i++) {
            // Rodrigues' rotation: rotate vertex around (ax,ay,az) by spinA
            float vx = verts[i][0], vy = verts[i][1], vz = verts[i][2];
            float vx_c = ay * vz - az * vy;
            float vy_c = az * vx - ax * vz;
            float vz_c = ax * vy - ay * vx;
            float ax_vc = ay * vz_c - az * vy_c;
            float ay_vc = az * vx_c - ax * vz_c;
            float az_vc = ax * vy_c - ay * vx_c;
            float rx = vx + sinA * vx_c + (1.0F - cosA) * ax_vc;
            float ry = vy + sinA * vy_c + (1.0F - cosA) * ay_vc;
            float rz = vz + sinA * vz_c + (1.0F - cosA) * az_vc;

            float[] p = projectToScreen(cube.wx + rx, cube.wy + ry, cube.wz + rz);
            scr[i][0] = p[0];
            scr[i][1] = p[1];
            scr[i][2] = p[2];
        }

        // Pre-computed UVs for each face: {u1,v1, u2,v2, u3,v3, u4,v4}
        float[][] faceUVs = {
                {0, 1, 1, 1, 1, 0, 0, 0}, // -Y (bottom)
                {0, 0, 1, 0, 1, 1, 0, 1}, // +Y (top)
                {0, 1, 1, 1, 1, 0, 0, 0}, // -Z (back)
                {0, 1, 1, 1, 1, 0, 0, 0}, // +X (right)
                {0, 0, 1, 0, 1, 1, 0, 1}, // +Z (front)
                {0, 1, 1, 1, 1, 0, 0, 0}, // -X (left)
        };

        // Emit each face with per-vertex lighting + UV mapping.
        for (int f = 0; f < 6; f++) {
            float[] n = normals[f];
            // Rodrigues' rotation for normal
            float vx = n[0], vy = n[1], vz = n[2];
            float vx_c = ay * vz - az * vy;
            float vy_c = az * vx - ax * vz;
            float vz_c = ax * vy - ay * vx;
            float ax_vc = ay * vz_c - az * vy_c;
            float ay_vc = az * vx_c - ax * vz_c;
            float az_vc = ax * vy_c - ay * vx_c;
            float rx = vx + sinA * vx_c + (1.0F - cosA) * ax_vc;
            float ry = vy + sinA * vy_c + (1.0F - cosA) * ay_vc;
            float rz = vz + sinA * vz_c + (1.0F - cosA) * az_vc;

            // Lambertian: brightness based on dot(normal, lightDir)
            float dot = rx * cube.lightX + ry * cube.lightY + rz * cube.lightZ;
            float bri = cube.emissive ? 1.0F : 0.1F + 0.8F * Math.max(0.0F, dot);
            int br = (int)(bri * 255.0F);
            int cr = (int)(cube.r * br);
            int cg = (int)(cube.g * br);
            int cb = (int)(cube.b * br);

            int[] idx = faces[f];
            float[] uvs = faceUVs[f];
            for (int i = 0; i < 4; i++) {
                float vertexZ = -1000.0F + 2000.0F * (scr[idx[i]][2] - pMin) / span;
                bb.vertex(CUBE_IDENTITY, scr[idx[i]][0], scr[idx[i]][1], vertexZ)
                        .uv(uvs[i * 2], uvs[i * 2 + 1])
                        .color(cr, cg, cb, 255)
                        .endVertex();
            }
        }
    }

    /** Pack r/g/b/a floats into ARGB int. */
    private static int rgba(float r, float g, float b, float a) {
        return ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    /**
     * Compute the local orbit offset (ox, oy, oz) of a body at game time {@code t}
     * from its {@link OrbitData} around the parent body.
     *
     * <p>Circular orbits use plain trigonometry; elliptical orbits (comets) are
     * solved with Kepler's equation (Newton iteration).  The orbit plane lies
     * in XZ by default and can be tilted via inclination / ascending node.</p>
     */
    private float[] orbitOffset(OrbitData orbit, float t) {
        return orbitOffset(orbit, t, 0.0F, 1.0F, 0.0F);
    }

    /**
     * Compute the local orbit offset (ox, oy, oz) of a body at game time {@code t}
     * from its {@link OrbitData} around the parent body, with per-rock overrides.
     *
     * @param extraPhase  additional phase shift along the orbit (radians)
     * @param radiusScale multiplier for the semi-major axis (rock scatter)
     * @param yExtra      extra vertical offset (applied before inclination)
     */
    private float[] orbitOffset(OrbitData orbit, float t, float extraPhase, float radiusScale, float yExtra) {
        float a = Math.max(0.1F, orbit.semiMajorAxis() * radiusScale);
        float e = Mth.clamp(orbit.eccentricity(), 0.0F, 0.95F);
        float inc = orbit.inclination() * Mth.DEG_TO_RAD;
        float node = orbit.ascendingNode() * Mth.DEG_TO_RAD;

        // Mean anomaly — use modulo of the period to avoid float precision loss
        long period = Math.max(1L, orbit.period());
        float M = (float)(Math.PI * 2.0 * ((t % period) / (double)period)) + orbit.phase() + extraPhase;

        float E;
        if (e > 0.001F) {
            // Solve Kepler's equation M = E - e*sin(E) via Newton iteration
            E = M;
            for (int i = 0; i < 8; i++) {
                E = E - (E - e * (float)Math.sin(E) - M) / (1.0F - e * (float)Math.cos(E));
            }
        } else {
            E = M;
        }

        // True anomaly
        float nu = 2.0F * (float)Math.atan2(
                Math.sqrt(1.0 + e) * Math.sin(E / 2.0),
                Math.sqrt(1.0 - e) * Math.cos(E / 2.0));
        float r = a * (1.0F - e * (float)Math.cos(E));

        float x = r * Mth.cos(nu);
        float z = r * Mth.sin(nu);
        float y = yExtra;

        if (inc != 0.0F || node != 0.0F) {
            // Rotate around X by inclination, then around Y by ascending node
            float y1 = y * Mth.cos(inc) - z * Mth.sin(inc);
            float z1 = y * Mth.sin(inc) + z * Mth.cos(inc);
            float x2 = x * Mth.cos(node) + z1 * Mth.sin(node);
            float z2 = -x * Mth.sin(node) + z1 * Mth.cos(node);
            return new float[]{x2, y1, z2};
        }
        return new float[]{x, y, z};
    }

    /** World position of a star at current time (may orbit a barycenter). */
    private float[] starWorldPos(SystemRenderInfo sys, StarRenderInfo star) {
        if (star.barycenter != null) {
            float[] off = orbitOffset(star.orbit, this.timeOfDay);
            return new float[]{
                    (float)sys.worldX + (float)star.barycenter.getX() + off[0],
                    (float)sys.worldY + (float)star.barycenter.getY() + off[1],
                    (float)sys.worldZ + (float)star.barycenter.getZ() + off[2]
            };
        }
        return new float[]{(float)sys.worldX, (float)sys.worldY, (float)sys.worldZ};
    }

    /** World position of a body orbiting around the parent point (px, py, pz). */
    private float[] bodyWorldPos(float px, float py, float pz, BodyRenderInfo body) {
        float[] off = orbitOffset(body.orbit, this.timeOfDay);
        return new float[]{px + off[0], py + off[1], pz + off[2]};
    }

    /** Normalized direction vector from (x1,y1,z1) to (x2,y2,z2). */
    private static float[] dirTo(float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-4F) return new float[]{0.0F, 0.0F, 0.0F};
        return new float[]{dx / len, dy / len, dz / len};
    }

    /**
     * Find the current world position of any body by its name.
     * Returns null when the body is not present on the map.
     */
    private float[] findBodyWorldPos(String name) {
        for (SystemRenderInfo sys : systems) {
            for (StarRenderInfo star : sys.stars) {
                float[] starW = starWorldPos(sys, star);
                if (star.name.equals(name)) return starW;

                for (BodyRenderInfo planet : star.planets) {
                    float[] pw = bodyWorldPos(starW[0], starW[1], starW[2], planet);
                    if (planet.name.equals(name)) return pw;

                    for (BodyRenderInfo child : planet.children) {
                        float[] cw = bodyWorldPos(pw[0], pw[1], pw[2], child);
                        if (child.name.equals(name)) return cw;
                    }
                }

                for (BodyRenderInfo asteroid : star.asteroids) {
                    float[] aw = bodyWorldPos(starW[0], starW[1], starW[2], asteroid);
                    if (asteroid.name.equals(name)) return aw;
                }

                for (BodyRenderInfo comet : star.comets) {
                    float[] cw = bodyWorldPos(starW[0], starW[1], starW[2], comet);
                    if (comet.name.equals(name)) return cw;
                }
            }
        }
        return null;
    }

/**
     * Draw the full orbit of a body as dots.  The orbit shape follows its
     * {@link OrbitData}: circular, elliptical or inclined.  The dot size is
     * controlled by {@code orbitWidth} from {@link OrbitVisual} (1 = thin, 2..4 = thicker).
     */
    private void drawOrbitRing(GuiGraphics guiGraphics, float cx, float cy, float cz,
                               OrbitData orbit, OrbitVisual visual, int color) {
        int segments = 1024;
        int w = Mth.clamp(visual.orbitWidth(), 1, 4);
        for (int i = 0; i < segments; i++) {
            float t = (float)Math.max(1L, orbit.period()) * (float)i / (float)segments;
            float[] off = orbitOffset(orbit, t);
            float[] s = toScreen(cx + off[0], cy + off[1], cz + off[2], 0);
            if (s[0] >= 0 && s[0] <= width && s[1] >= 0 && s[1] <= height) {
                guiGraphics.fill((int)s[0], (int)s[1], (int)s[0] + w, (int)s[1] + w, color);
            }
        }
    }


    /**
     * Render a comet's tail: a fading trail of dots pointing away from its
     * parent star (solar wind direction).
     */
    private void renderCometTail(GuiGraphics guiGraphics, float starX, float starY, float starZ,
                                 float cometX, float cometY, float cometZ) {
        float[] away = dirTo(starX, starY, starZ, cometX, cometY, cometZ);
        if (away[0] == 0.0F && away[1] == 0.0F && away[2] == 0.0F) return;

        int segs = 14;
        for (int i = 1; i <= segs; i++) {
            float dist = i * 0.4F;
            float[] p = toScreen(
                    cometX - away[0] * dist,
                    cometY - away[1] * dist,
                    cometZ - away[2] * dist, 0);
            int alpha = (int)(110 * (1.0F - (float)i / segs));
            int color = (alpha << 24) | 0xAFE8FF;
            int sz = Math.max(1, 3 - i / 4);
            guiGraphics.fill((int)p[0] - sz, (int)p[1] - sz, (int)p[0] + sz, (int)p[1] + sz, color);
        }
    }

    // ── Hover detection ───────────────────────────────────────────────────

    private void checkHover(int mouseX, int mouseY, float half) {
        float closestDist = 8.0F;
        String closest = null;

        for (SystemRenderInfo sys : systems) {
            for (StarRenderInfo star : sys.stars) {
                float[] starW = starWorldPos(sys, star);
                float[] s = toScreen(starW[0], starW[1], starW[2], half);
                float d = dst(mouseX, mouseY, s[0], s[1]);
                if (d < closestDist) { closestDist = d; closest = star.name; }

                for (BodyRenderInfo planet : star.planets) {
                    float[] pw = bodyWorldPos(starW[0], starW[1], starW[2], planet);
                    float[] s2 = toScreen(pw[0], pw[1], pw[2], half);
                    d = dst(mouseX, mouseY, s2[0], s2[1]);
                    if (d < closestDist) { closestDist = d; closest = planet.name; }

                    for (BodyRenderInfo child : planet.children) {
                        float[] cw = bodyWorldPos(pw[0], pw[1], pw[2], child);
                        float[] s3 = toScreen(cw[0], cw[1], cw[2], half);
                        d = dst(mouseX, mouseY, s3[0], s3[1]);
                        if (d < closestDist) { closestDist = d; closest = child.name; }
                    }
                }

                for (BodyRenderInfo asteroid : star.asteroids) {
                    float[] aw = bodyWorldPos(starW[0], starW[1], starW[2], asteroid);
                    float[] s4 = toScreen(aw[0], aw[1], aw[2], half);
                    d = dst(mouseX, mouseY, s4[0], s4[1]);
                    if (d < closestDist) { closestDist = d; closest = asteroid.name; }
                }

                for (BodyRenderInfo comet : star.comets) {
                    float[] cw = bodyWorldPos(starW[0], starW[1], starW[2], comet);
                    float[] s5 = toScreen(cw[0], cw[1], cw[2], half);
                    d = dst(mouseX, mouseY, s5[0], s5[1]);
                    if (d < closestDist) { closestDist = d; closest = comet.name; }
                }
            }
        }

        if (closest != null) {
            this.hoveredBodyName = closest;
        } else {
            this.hoveredBodyName = null;
        }
    }

    private static float dst(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // ── Mouse handling ────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // LMB (button 0): select body or deselect
        if (button == 0) {
            boolean hit = selectBodyAt((int)mouseX, (int)mouseY);
            if (!hit) {
                // Clicked on empty space — deselect
                selectedBody = null;
                followSelected = false;
            }
            return true;
        }
        // RMB (button 1): start rotation
        if (button == 1) {
            isRotating = true;
            rotStartX = mouseX;
            rotStartY = mouseY;
            // Grab the cursor so the rotation is not limited by the window edges
            lockCursor();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // MMB (button 2): pan the map
        if (button == 2) {
            if (!isPanning) {
                isPanning = true;
                panStartX = mouseX;
                panStartY = mouseY;
            }
            panOffsetX += (float)(mouseX - panStartX) * 0.3F;
            panOffsetY += (float)(mouseY - panStartY) * 0.3F;
            panStartX = mouseX;
            panStartY = mouseY;
            return true;
        }
        // RMB (button 1): rotate the camera — incremental deltas allow free 360° rotation
        if (button == 1 && isRotating) {
            float dx = (float)(mouseX - rotStartX);
            float dy = (float)(mouseY - rotStartY);
            camRotY += dx * 0.5F;
            camRotX = Mth.clamp(camRotX + dy * 0.5F, -90.0F, 90.0F);
            rotStartX = mouseX;
            rotStartY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 2) isPanning = false;
        if (button == 1) {
            isRotating = false;
            // Release the cursor so normal mouse interaction resumes
            unlockCursor();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Hide & grab the mouse cursor so dragging is not limited by the window bounds. */
    private void lockCursor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() != null) {
            GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        }
    }

    /** Restore the normal visible cursor. */
    private void unlockCursor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() != null) {
            GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    @Override
    public void removed() {
        // Make sure the cursor is always restored when leaving the screen
        unlockCursor();
        super.removed();
    }

    @Override
    public void onClose() {
        unlockCursor();
        super.onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Scroll wheel: zoom in/out
        zoom = Mth.clamp(zoom * (float) (1.0 - delta * 0.1), 0.15F, 5.0F);
        return true;
    }

    /** Find and select the body at screen position (sx, sy). Returns true if a body was hit. */
    private boolean selectBodyAt(int sx, int sy) {
        float closestDist = 10.0F;
        SelectedBody closest = null;

        for (SystemRenderInfo sys : systems) {
            for (StarRenderInfo star : sys.stars) {
                float[] starW = starWorldPos(sys, star);
                float[] s = toScreen(starW[0], starW[1], starW[2], 0);
                float d = dst(sx, sy, s[0], s[1]);
                if (d < closestDist) {
                    closestDist = d;
                    closest = new SelectedBody(star.name, "star", starW[0], starW[1], starW[2]);
                }

                for (BodyRenderInfo planet : star.planets) {
                    float[] pw = bodyWorldPos(starW[0], starW[1], starW[2], planet);
                    float[] s2 = toScreen(pw[0], pw[1], pw[2], 0);
                    d = dst(sx, sy, s2[0], s2[1]);
                    if (d < closestDist) {
                        closestDist = d;
                        closest = new SelectedBody(planet.name, "planet", pw[0], pw[1], pw[2]);
                    }

                    for (BodyRenderInfo child : planet.children) {
                        float[] cw = bodyWorldPos(pw[0], pw[1], pw[2], child);
                        float[] s3 = toScreen(cw[0], cw[1], cw[2], 0);
                        d = dst(sx, sy, s3[0], s3[1]);
                        if (d < closestDist) {
                            closestDist = d;
                            closest = new SelectedBody(child.name, child.type, cw[0], cw[1], cw[2]);
                        }
                    }
                }

                for (BodyRenderInfo asteroid : star.asteroids) {
                    float[] aw = bodyWorldPos(starW[0], starW[1], starW[2], asteroid);
                    float[] s4 = toScreen(aw[0], aw[1], aw[2], 0);
                    d = dst(sx, sy, s4[0], s4[1]);
                    if (d < closestDist) {
                        closestDist = d;
                        closest = new SelectedBody(asteroid.name, asteroid.type, aw[0], aw[1], aw[2]);
                    }
                }

                for (BodyRenderInfo comet : star.comets) {
                    float[] cw = bodyWorldPos(starW[0], starW[1], starW[2], comet);
                    float[] s5 = toScreen(cw[0], cw[1], cw[2], 0);
                    d = dst(sx, sy, s5[0], s5[1]);
                    if (d < closestDist) {
                        closestDist = d;
                        closest = new SelectedBody(comet.name, comet.type, cw[0], cw[1], cw[2]);
                    }
                }
            }
        }

        if (closest != null) {
            selectedBody = closest;
            followSelected = true;
            // Center camera on selected body
            centerOnBody(closest);
            return true;
        }
        return false;
    }

    /** Center the camera on a selected body. */
    private void centerOnBody(SelectedBody body) {
        // Use the body's world position as camera offset
        camOffsetX = (float)body.worldX;
        camOffsetY = (float)body.worldY;
        camOffsetZ = (float)body.worldZ;
        // Reset pan offset so it doesn't interfere
        panOffsetX = 0.0F;
        panOffsetY = 0.0F;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Zoom with + / - keys
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_EQUAL || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ADD) {
            zoom = Mth.clamp(zoom * 1.2F, 0.15F, 5.0F);
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_MINUS || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_SUBTRACT) {
            zoom = Mth.clamp(zoom / 1.2F, 0.15F, 5.0F);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── Catalog rendering ────────────────────────────────────────────────

    /** Render the catalog panel on the left side of the screen. */
    private void renderCatalog(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        int top = 30;
        int x = CATALOG_X;
        int w = CATALOG_WIDTH;

        // Calculate total height needed
        int totalHeight = CATALOG_HEADER_HEIGHT + catalog.size() * CATALOG_ITEM_HEIGHT;
        int visibleHeight = CATALOG_HEADER_HEIGHT + CATALOG_MAX_VISIBLE * CATALOG_ITEM_HEIGHT;

        // Draw catalog background
        guiGraphics.fill(x, top, x + w, top + totalHeight, 0xCC0D0D1F);

        // Draw catalog border (blue)
        guiGraphics.renderOutline(x, top, w, totalHeight, 0x3030A0);

        // Header "CATALOG"
        String header = Component.translatable("gui.stellarstory.catalog.title").getString();
        int headerX = x + (w - font.width(header)) / 2;
        guiGraphics.drawString(font, header, headerX, top + 4, 0xAAAAFF, false);

        // Header bottom line
        guiGraphics.fill(x, top + CATALOG_HEADER_HEIGHT - 1, x + w, top + CATALOG_HEADER_HEIGHT, 0x3030A0);

        // Draw catalog items (scrollable)
        int startY = top + CATALOG_HEADER_HEIGHT;
        int itemCount = catalog.size();
        int visibleCount = Math.min(CATALOG_MAX_VISIBLE, itemCount);

        for (int i = 0; i < visibleCount; i++) {
            int entryY = startY + i * CATALOG_ITEM_HEIGHT;
            CatalogEntry entry = catalog.get(i);

            // Check if this item is hovered
            boolean isHovered = mouseX >= x && mouseX <= x + w && mouseY >= entryY && mouseY < entryY + CATALOG_ITEM_HEIGHT;

            // Item background (highlight on hover)
            int bgAlpha = isHovered ? 0x80 : 0x40;
            guiGraphics.fill(x + 2, entryY + 1, x + w - 2, entryY + CATALOG_ITEM_HEIGHT - 1,
                    bgAlpha | 0x151525);

            // Color indicator bar (left side)
            guiGraphics.fill(x + 3, entryY + 3, x + 6, entryY + CATALOG_ITEM_HEIGHT - 3, entry.color);

            // Item text
            String itemName = entry.name;
            int textX = x + 12;
            int textY = entryY + (CATALOG_ITEM_HEIGHT - 8) / 2;
            int textColor = selectedBody != null && selectedBody.name.equals(entry.name)
                    ? 0xFFFF88
                    : 0xDDDDDD;
            guiGraphics.drawString(font, itemName, textX, textY, textColor, false);
        }

        // Scrollbar if needed
        if (itemCount > CATALOG_MAX_VISIBLE) {
            int scrollbarX = x + w - 4;
            int scrollbarY = startY + 2;
            int scrollbarH = visibleHeight - 4;
            float ratio = (float) CATALOG_MAX_VISIBLE / itemCount;
            int thumbH = Math.max(20, (int)(scrollbarH * ratio));
            int thumbY = scrollbarY + (int)((scrollbarH - thumbH) * 0.0F); // scroll offset 0 for now

            // Scrollbar background
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 3, scrollbarY + scrollbarH, 0x60303050);
            // Scrollbar thumb
            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 3, thumbY + thumbH, 0x80505080);
        }
    }
}