package stellarstory.client.renderer.sky.helper;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class SkyHelper {

    // =====================================================================
    //  Параметры положения и ориентации небесного тела
    // =====================================================================

    /**
     * Параметры положения и ориентации небесного тела на небесной сфере.
     * Поддерживает два режима: фиксированное положение (elevation/azimuth)
     * и орбитальное движение (speed, phase, inclination).
     *
     * <h3>Примеры использования</h3>
     * <pre>{@code
     * // Фиксированная планета в зените
     * SkyHelper.BodyParams planet = BodyParams.fixed(0.0F, 0.0F, 100.0F)
     *         .tilt(23.5F, 0.0F).spin(spinDeg);
     *
     * // Луна на орбите с периодом 24000 тиков (1 день), фаза 0.5 (противоположно солнцу)
     * SkyHelper.BodyParams moon = BodyParams.orbit(100.0F, 1.0F, 0.5F, 5.14F)
     *         .tilt(1.54F, 0.0F).spin(spinDeg);
     *
     * // Медленный спутник (период 10 дней)
     * SkyHelper.BodyParams slowSat = BodyParams.orbit(120.0F, 0.1F, 0.0F, 0.0F)
     *         .tilt(0.0F, 0.0F).spin(0.0F);
     * }</pre>
     */
    public static final class BodyParams {
        public boolean orbit;
        public float elevationDeg = 0.0F;
        public float azimuthDeg = 0.0F;
        public float distance = 100.0F;
        public float orbitSpeed = 1.0F;       // оборотов за игровой день (1 = как солнце)
        public float phaseOffset = 0.0F;      // доля круга
        public float inclination = 0.0F;      // наклон плоскости орбиты в градусах
        public float tiltX = 0.0F;            // наклон оси X (world-space)
        public float tiltZ = 0.0F;            // наклон оси Z (world-space)
        public float spinDeg = 0.0F;          // самоповорот вокруг локальной Y

        /** Фиксированное положение в небе. */
        public static BodyParams fixed(float elevationDeg, float azimuthDeg, float distance) {
            BodyParams p = new BodyParams();
            p.orbit = false;
            p.elevationDeg = elevationDeg;
            p.azimuthDeg = azimuthDeg;
            p.distance = distance;
            return p;
        }

        /** Орбитальное движение. speed = 1.0 → один оборот за игровой день. */
        public static BodyParams orbit(float distance, float orbitSpeed, float phaseOffset, float inclination) {
            BodyParams p = new BodyParams();
            p.orbit = true;
            p.distance = distance;
            p.orbitSpeed = orbitSpeed;
            p.phaseOffset = phaseOffset;
            p.inclination = inclination;
            return p;
        }

        public BodyParams tilt(float tiltX, float tiltZ) { this.tiltX = tiltX; this.tiltZ = tiltZ; return this; }
        public BodyParams spin(float spinDeg) { this.spinDeg = spinDeg; return this; }
    }

    /**
     * Параметры направления источника света (звезды).
     * Для стандартного солнца (период 1 день, фаза 0, наклон 0) достаточно null —
     * используется дефолтное направление из timeOfDay.
     */
    public static final class LightParams {
        public boolean orbit;
        public float elevationDeg = 0.0F;
        public float azimuthDeg = 0.0F;
        public float orbitSpeed = 1.0F;
        public float phaseOffset = 0.0F;
        public float inclination = 0.0F;

        public static LightParams fixed(float elevationDeg, float azimuthDeg) {
            LightParams p = new LightParams();
            p.orbit = false;
            p.elevationDeg = elevationDeg;
            p.azimuthDeg = azimuthDeg;
            return p;
        }

        public static LightParams orbit(float orbitSpeed, float phaseOffset, float inclination) {
            LightParams p = new LightParams();
            p.orbit = true;
            p.orbitSpeed = orbitSpeed;
            p.phaseOffset = phaseOffset;
            p.inclination = inclination;
            return p;
        }
    }

    /**
     * Направление на небесное тело в кадровой системе координат неба.
     * Для orbit=false: elevation/azimuth → сферические координаты.
     * Для orbit=true: орбитальный поворот × (0,1,0).
     */
    public static Vector3f bodyDirection(BodyParams body, float timeOfDay) {
        if (!body.orbit) {
            float el = body.elevationDeg * Mth.DEG_TO_RAD;
            float az = body.azimuthDeg * Mth.DEG_TO_RAD;
            return new Vector3f(Mth.cos(el) * Mth.sin(az), Mth.sin(el), Mth.cos(el) * Mth.cos(az));
        }
        Matrix4f m = new Matrix4f();
        m.rotateY(-90.0F * Mth.DEG_TO_RAD);
        if (body.inclination != 0.0F) m.rotateZ(body.inclination * Mth.DEG_TO_RAD);
        m.rotateX((timeOfDay * body.orbitSpeed + body.phaseOffset) * 360.0F * Mth.DEG_TO_RAD);
        Vector3f d = new Vector3f(0.0F, 1.0F, 0.0F);
        m.transformDirection(d);
        return d.normalize();
    }

    /** Направление на источник света (звезду). null → стандартное солнце (speed=1, phase=0, incl=0). */
    public static Vector3f lightDirection(@Nullable LightParams light, float timeOfDay) {
        if (light == null) {
            return sunDirection(timeOfDay);
        }
        if (!light.orbit) {
            float el = light.elevationDeg * Mth.DEG_TO_RAD;
            float az = light.azimuthDeg * Mth.DEG_TO_RAD;
            return new Vector3f(Mth.cos(el) * Mth.sin(az), Mth.sin(el), Mth.cos(el) * Mth.cos(az));
        }
        Matrix4f m = new Matrix4f();
        m.rotateY(-90.0F * Mth.DEG_TO_RAD);
        if (light.inclination != 0.0F) m.rotateZ(light.inclination * Mth.DEG_TO_RAD);
        m.rotateX((timeOfDay * light.orbitSpeed + light.phaseOffset) * 360.0F * Mth.DEG_TO_RAD);
        Vector3f d = new Vector3f(0.0F, 1.0F, 0.0F);
        m.transformDirection(d);
        return d.normalize();
    }

    /** Матрица ориентации куба: наклон оси + самоповорот. Используется для освещения. */
    public static Matrix4f bodyOrientation(BodyParams body) {
        Matrix4f m = new Matrix4f();
        m.rotateX(body.tiltX * Mth.DEG_TO_RAD);
        m.rotateZ(body.tiltZ * Mth.DEG_TO_RAD);
        m.rotateY(body.spinDeg * Mth.DEG_TO_RAD);
        return m;
    }

    /** Lambertian brightness: dot(faceNormal, lightDir). */
    private static float faceBrightness(float nx, float ny, float nz, Vector3f light) {
        float dot = nx * light.x + ny * light.y + nz * light.z;
        return 0.05F + 0.75F * Math.max(0.0F, dot);
    }

    /** Per-face brightness [ +Z, -Z, +X, -X, +Y, -Y ]. */
    private static float[] computeBrightness(Matrix4f orientation, Vector3f lightDir) {
        Vector3f local = new Vector3f(lightDir);
        new Matrix4f(orientation).invert().transformDirection(local);
        local.normalize();
        return new float[]{
                faceBrightness( 0,  0,  1, local),
                faceBrightness( 0,  0, -1, local),
                faceBrightness( 1,  0,  0, local),
                faceBrightness(-1,  0,  0, local),
                faceBrightness( 0,  1,  0, local),
                faceBrightness( 0, -1,  0, local),
        };
    }

    // =====================================================================

    @Nullable
    private VertexBuffer skyBuffer;

    @Nullable
    private VertexBuffer darkBuffer;

    @Nullable
    private VertexBuffer cubeBuffer;

    public VertexBuffer getSkyBuffer() {
        return this.skyBuffer;
    }

    public VertexBuffer getDarkBuffer() {
        return this.darkBuffer;
    }

    public VertexBuffer getCubeBuffer() {
        return this.cubeBuffer;
    }

    public void createLightSky() {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        if (this.skyBuffer != null) {
            this.skyBuffer.close();
        }

        this.skyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer = buildSkyDisc(bufferbuilder, 16.0F);
        this.skyBuffer.bind();
        this.skyBuffer.upload(bufferbuilder$renderedbuffer);
        VertexBuffer.unbind();
    }

    public void createDarkSky() {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        if (this.darkBuffer != null) {
            this.darkBuffer.close();
        }

        this.darkBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer = buildSkyDisc(bufferbuilder, -16.0F);
        this.darkBuffer.bind();
        this.darkBuffer.upload(bufferbuilder$renderedbuffer);
        VertexBuffer.unbind();
    }

    public void renderSky(VertexBuffer starBuffer, Matrix4f matrix, PoseStack poseStack, Minecraft minecraft, float partialTicks, float sunBrightness, float starBrightness, float orbitInclination, Runnable setupFog) {
        Vec3 vec3 = minecraft.level.getSkyColor(minecraft.gameRenderer.getMainCamera().getPosition(), partialTicks);
        float f = (float)vec3.x;
        float f1 = (float)vec3.y;
        float f2 = (float)vec3.z;
        FogRenderer.levelFogColor();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(f, f1, f2, 1.0F);
        ShaderInstance shaderinstance = RenderSystem.getShader();
        this.skyBuffer.bind();
        this.skyBuffer.drawWithShader(poseStack.last().pose(), matrix, shaderinstance);
        VertexBuffer.unbind();
        RenderSystem.enableBlend();
        float[] afloat = minecraft.level.effects().getSunriseColor(minecraft.level.getTimeOfDay(partialTicks), partialTicks);
        if (afloat != null) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.pushPose();

            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            if (orbitInclination != 0.0F) {
                poseStack.mulPose(Axis.YP.rotationDegrees(orbitInclination));
            }
            float f3 = Mth.sin(minecraft.level.getSunAngle(partialTicks)) < 0.0F ? 180.0F : 0.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(f3));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            float f4 = afloat[0];
            float f5 = afloat[1];
            float f6 = afloat[2];
            Matrix4f matrix4f = poseStack.last().pose();
            bufferbuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            bufferbuilder.vertex(matrix4f, 0.0F, 100.0F, 0.0F).color(f4, f5, f6, afloat[3]).endVertex();
            int i = 16;

            for(int j = 0; j <= 16; ++j) {
                float f7 = (float)j * ((float)Math.PI * 2F) / 16.0F;
                float f8 = Mth.sin(f7);
                float f9 = Mth.cos(f7);
                bufferbuilder.vertex(matrix4f, f8 * 120.0F, f9 * 120.0F, -f9 * 40.0F * afloat[3]).color(afloat[0], afloat[1], afloat[2], 0.0F).endVertex();
            }

            BufferUploader.drawWithShader(bufferbuilder.end());
            poseStack.popPose();
        }
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        poseStack.pushPose();
        float alpha = sunBrightness;//1.0F - minecraft.level.getRainLevel(partialTicks);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        if (orbitInclination != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(orbitInclination));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(minecraft.level.getTimeOfDay(partialTicks) * 360.0F));
        Matrix4f matrix4f1 = poseStack.last().pose();
        float f12 = 30.0F;
        /*
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, sunTexture);
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix4f1, -f12, 100.0F, -f12).uv(0.0F, 0.0F).endVertex();
        bufferbuilder.vertex(matrix4f1, f12, 100.0F, -f12).uv(1.0F, 0.0F).endVertex();
        bufferbuilder.vertex(matrix4f1, f12, 100.0F, f12).uv(1.0F, 1.0F).endVertex();
        bufferbuilder.vertex(matrix4f1, -f12, 100.0F, f12).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
        */
        f12 = 20.0F;
        float f10 = starBrightness;//minecraft.level.getStarBrightness(partialTicks) * alpha;

        if (f10 > 0.0F) {
            RenderSystem.setShaderColor(f10, f10, f10, f10);
            FogRenderer.setupNoFog();
            starBuffer.bind();
            starBuffer.drawWithShader(poseStack.last().pose(), matrix, GameRenderer.getPositionShader());
            VertexBuffer.unbind();
            setupFog.run();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        poseStack.popPose();

        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        double d0 = minecraft.player.getEyePosition(partialTicks).y - minecraft.level.getLevelData().getHorizonHeight(minecraft.level);
        if (d0 < 0.0D) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 12.0F, 0.0F);
            this.darkBuffer.bind();
            this.darkBuffer.drawWithShader(poseStack.last().pose(), matrix, shaderinstance);
            VertexBuffer.unbind();
            poseStack.popPose();
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
    }

    /**
     * Builds a textured cube vertex buffer centered at the origin.
     * All 6 faces use CW winding (matching Minecraft sky convention) with full UV mapping.
     *
     * @param halfSize half-extent of the cube (e.g. 15.0F for a 30x30x30 cube)
     */
    public void createCube(float halfSize) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        if (this.cubeBuffer != null) {
            this.cubeBuffer.close();
        }

        this.cubeBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        BufferBuilder.RenderedBuffer rendered = buildCube(builder, halfSize);
        this.cubeBuffer.bind();
        this.cubeBuffer.upload(rendered);
        VertexBuffer.unbind();
    }

    /**
     * Renders a textured cube at the given position with rotation, scale, and alpha.
     * Supports either 1 texture (applied to all faces, uses fast VertexBuffer path) or
     * 6 textures (one per face: +Z, -Z, +X, -X, +Y, -Y, immediate mode).
     *
     * @param textures either 1 texture for all faces, or 6 textures (one per face)
     */
    public void renderCube(PoseStack poseStack,
                           float x, float y, float z,
                           float rotX, float rotY, float rotZ,
                           float scale, float timeOfDay,
                           float orbitPhaseOffset, float orbitInclination,
                           float r, float g, float b, float alpha,
                           ResourceLocation... textures) {
        boolean multiTex = textures.length >= 6;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(r, g, b, alpha);

        if (!multiTex) {
            RenderSystem.setShaderTexture(0, textures[0]);
        }

        poseStack.pushPose();
        applyOrbitRotations(poseStack, timeOfDay, orbitPhaseOffset, orbitInclination);
        applyCubeTransforms(poseStack, x, y, z, scale, rotX, rotY, rotZ);

        if (!multiTex && this.cubeBuffer != null) {
            // Fast path: single draw call with pre-built buffer
            this.cubeBuffer.bind();
            this.cubeBuffer.drawWithShader(poseStack.last().pose(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
        } else {
            // Multi-texture path: per-face immediate mode
            Matrix4f mat = poseStack.last().pose();
            for (int i = 0; i < 6; i++) {
                if (multiTex) {
                    RenderSystem.setShaderTexture(0, textures[i]);
                }

                BufferBuilder buf = Tesselator.getInstance().getBuilder();
                buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

                float[] v = CUBE_FACES[i];
                buf.vertex(mat, v[0], v[1], v[2]).uv(0, 1).endVertex();
                buf.vertex(mat, v[3], v[4], v[5]).uv(1, 1).endVertex();
                buf.vertex(mat, v[6], v[7], v[8]).uv(1, 0).endVertex();
                buf.vertex(mat, v[9], v[10], v[11]).uv(0, 0).endVertex();

                BufferUploader.drawWithShader(buf.end());
            }
        }

        poseStack.popPose();
    }

    private static BufferBuilder.RenderedBuffer buildCube(BufferBuilder builder, float s) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        // +Z face (front) — CW viewed from +Z
        builder.vertex(-s, -s,  s).uv(0.0F, 1.0F).endVertex();
        builder.vertex( s, -s,  s).uv(1.0F, 1.0F).endVertex();
        builder.vertex( s,  s,  s).uv(1.0F, 0.0F).endVertex();
        builder.vertex(-s,  s,  s).uv(0.0F, 0.0F).endVertex();

        // -Z face (back) — CW viewed from -Z
        builder.vertex( s, -s, -s).uv(0.0F, 1.0F).endVertex();
        builder.vertex(-s, -s, -s).uv(1.0F, 1.0F).endVertex();
        builder.vertex(-s,  s, -s).uv(1.0F, 0.0F).endVertex();
        builder.vertex( s,  s, -s).uv(0.0F, 0.0F).endVertex();

        // +X face (right) — CW viewed from +X
        builder.vertex( s, -s,  s).uv(0.0F, 1.0F).endVertex();
        builder.vertex( s, -s, -s).uv(1.0F, 1.0F).endVertex();
        builder.vertex( s,  s, -s).uv(1.0F, 0.0F).endVertex();
        builder.vertex( s,  s,  s).uv(0.0F, 0.0F).endVertex();

        // -X face (left) — CW viewed from -X
        builder.vertex(-s, -s, -s).uv(0.0F, 1.0F).endVertex();
        builder.vertex(-s, -s,  s).uv(1.0F, 1.0F).endVertex();
        builder.vertex(-s,  s,  s).uv(1.0F, 0.0F).endVertex();
        builder.vertex(-s,  s, -s).uv(0.0F, 0.0F).endVertex();

        // +Y face (top) — CW viewed from +Y
        builder.vertex(-s,  s,  s).uv(0.0F, 1.0F).endVertex();
        builder.vertex( s,  s,  s).uv(1.0F, 1.0F).endVertex();
        builder.vertex( s,  s, -s).uv(1.0F, 0.0F).endVertex();
        builder.vertex(-s,  s, -s).uv(0.0F, 0.0F).endVertex();

        // -Y face (bottom) — CW viewed from -Y
        builder.vertex(-s, -s, -s).uv(0.0F, 1.0F).endVertex();
        builder.vertex( s, -s, -s).uv(1.0F, 1.0F).endVertex();
        builder.vertex( s, -s,  s).uv(1.0F, 0.0F).endVertex();
        builder.vertex(-s, -s,  s).uv(0.0F, 0.0F).endVertex();

        return builder.end();
    }

    /** Unit cube face vertex data: {x1,y1,z1, x2,y2,z2, x3,y3,z3, x4,y4,z4} per face, CW winding. */
    private static final float[][] CUBE_FACES = {
        {-1,-1, 1,  1,-1, 1,  1, 1, 1, -1, 1, 1},   // +Z (front)
        { 1,-1,-1, -1,-1,-1, -1, 1,-1,  1, 1,-1},   // -Z (back)
        { 1,-1, 1,  1,-1,-1,  1, 1,-1,  1, 1, 1},   // +X (right)
        {-1,-1,-1, -1,-1, 1, -1, 1, 1, -1, 1,-1},   // -X (left)
        {-1, 1, 1,  1, 1, 1,  1, 1,-1, -1, 1,-1},   // +Y (top)
        {-1,-1,-1,  1,-1,-1,  1,-1, 1, -1,-1, 1},   // -Y (bottom)
    };

    /**
     * @deprecated Используйте {@link #renderLitBody(BodyParams, float, float, float, LightParams, ResourceLocation...)}
     * для корректной синхронизации освещения и положения тела.
     */
    @Deprecated
    public void renderLitCube(PoseStack poseStack,
                              float x, float y, float z,
                              float rotX, float rotY, float rotZ,
                              float scale, float alpha,
                              float timeOfDay, float elevationDeg, float azimuthDeg,
                              float orbitPhaseOffset, float orbitInclination,
                              ResourceLocation... textures) {
        float dist = (float) Math.sqrt(x*x + y*y + z*z);
        float elev = (float) Math.toDegrees(Math.asin(y / dist));
        float azim = (float) Math.toDegrees(Math.atan2(x, z));

        boolean orbiting = orbitPhaseOffset != 0.0F || orbitInclination != 0.0F;
        BodyParams bp;
        if (orbiting) {
            bp = BodyParams.orbit(dist, 1.0F, orbitPhaseOffset, orbitInclination)
                    .tilt(rotX, rotZ).spin(rotY);
        } else {
            bp = BodyParams.fixed(elev, azim, dist)
                    .tilt(rotX, rotZ).spin(rotY);
        }
        renderLitBody(poseStack, bp, scale, alpha, timeOfDay, null, textures);
    }

    /**
     * @deprecated Используйте {@link #renderLitBody(BodyParams, float, float, float, LightParams, ResourceLocation...)}
     */
    @Deprecated
    public void renderLitCubeFixed(PoseStack poseStack,
                                    float elevationDeg, float azimuthDeg, float distance,
                                    float tiltX, float tiltZ, float spinDeg,
                                    float size, float alpha,
                                    float sunTime,
                                    ResourceLocation... textures) {
        BodyParams bp = BodyParams.fixed(elevationDeg, azimuthDeg, distance)
                .tilt(tiltX, tiltZ).spin(spinDeg);
        renderLitBody(poseStack, bp, size, alpha, sunTime, null, textures);
    }

    /**
     * @deprecated Используйте {@link #renderAtmosphereBody(BodyParams, float, float, int, float, float, float, float, float, float, LightParams)}
     */
    @Deprecated
    public void renderAtmosphere(PoseStack poseStack,
                                  float x, float y, float z,
                                  float rotX, float rotY, float rotZ,
                                  float scale, float atmosphereScale, int layers,
                                  float r, float g, float b, float alpha, float brightness,
                                  float timeOfDay, float elevationDeg, float azimuthDeg,
                                  float orbitPhaseOffset, float orbitInclination) {
        float dist = (float) Math.sqrt(x*x + y*y + z*z);
        float elev = (float) Math.toDegrees(Math.asin(y / dist));
        float azim = (float) Math.toDegrees(Math.atan2(x, z));
        boolean orbiting = orbitPhaseOffset != 0.0F || orbitInclination != 0.0F;
        BodyParams bp;
        if (orbiting) {
            bp = BodyParams.orbit(dist, 1.0F, orbitPhaseOffset, orbitInclination)
                    .tilt(rotX, rotZ).spin(rotY);
        } else {
            bp = BodyParams.fixed(elev, azim, dist)
                    .tilt(rotX, rotZ).spin(rotY);
        }
        renderAtmosphereBody(poseStack, bp, scale, atmosphereScale, layers, r, g, b, alpha, brightness, timeOfDay, null);
    }

    /**
     * @deprecated Используйте {@link #renderAtmosphereBody(BodyParams, float, float, int, float, float, float, float, float, float, LightParams)}
     */
    @Deprecated
    public void renderAtmosphereFixed(PoseStack poseStack,
                                       float elevationDeg, float azimuthDeg, float distance,
                                       float tiltX, float tiltZ, float spinDeg,
                                       float size, float atmosphereScale, int layers,
                                       float r, float g, float b, float alpha, float brightness,
                                       float sunTime) {
        BodyParams bp = BodyParams.fixed(elevationDeg, azimuthDeg, distance)
                .tilt(tiltX, tiltZ).spin(spinDeg);
        renderAtmosphereBody(poseStack, bp, size, atmosphereScale, layers, r, g, b, alpha, brightness, sunTime, null);
    }

    // =====================================================================
    //  Универсальный рендер: fixed + orbit в одном методе
    // =====================================================================

    /**
     * Универсальный рендер освещённого куба. Работает для любого режима
     * положения тела (fixed / orbit) и любого источника света.
     *
     * <p>Освещение считается из одной матрицы ориентации куба (tilt + spin),
     * положение тела (fixed-направление или орбитальный угол) влияет только на
     * перевод (translate), но не на матрицу освещения. Это устраняет рассинхрон
     * между геометрией и терминатором.
     *
     * @param body     параметры положения/ориентации тела
     * @param size     радиус куба (половина ребра)
     * @param alpha    непрозрачность
     * @param timeOfDay level.getTimeOfDay(partialTicks)
     * @param light    параметры источника света; null = стандартное солнце
     * @param textures 1 текстура на все грани или 6 (+Z, -Z, +X, -X, +Y, -Y)
     */
    public void renderLitBody(PoseStack poseStack,
                              BodyParams body,
                              float size, float alpha,
                              float timeOfDay,
                              @Nullable LightParams light,
                              ResourceLocation... textures) {
        Vector3f lightDir = lightDirection(light, timeOfDay);
        Matrix4f orientation = bodyOrientation(body);
        float[] b = computeBrightness(orientation, lightDir);
        boolean multiTex = textures.length >= 6;

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (!multiTex) {
            RenderSystem.setShaderTexture(0, textures[0]);
        }

        Vector3f dir = bodyDirection(body, timeOfDay);
        poseStack.pushPose();
        poseStack.translate(dir.x * body.distance, dir.y * body.distance, dir.z * body.distance);
        if (size != 1.0F) poseStack.scale(size, size, size);
        poseStack.mulPose(Axis.XP.rotationDegrees(body.tiltX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(body.tiltZ));
        poseStack.mulPose(Axis.YP.rotationDegrees(body.spinDeg));

        Matrix4f mat = poseStack.last().pose();

        for (int i = 0; i < 6; i++) {
            if (multiTex) {
                RenderSystem.setShaderTexture(0, textures[i]);
            }

            BufferBuilder buf = Tesselator.getInstance().getBuilder();
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            float[] v = CUBE_FACES[i];
            buf.vertex(mat, v[0], v[1], v[2]).uv(0, 1).color(b[i], b[i], b[i], alpha).endVertex();
            buf.vertex(mat, v[3], v[4], v[5]).uv(1, 1).color(b[i], b[i], b[i], alpha).endVertex();
            buf.vertex(mat, v[6], v[7], v[8]).uv(1, 0).color(b[i], b[i], b[i], alpha).endVertex();
            buf.vertex(mat, v[9], v[10], v[11]).uv(0, 0).color(b[i], b[i], b[i], alpha).endVertex();

            BufferUploader.drawWithShader(buf.end());
        }

        poseStack.popPose();
    }

    /**
     * Атмосфера для универсального рендера. Использует ту же матрицу ориентации,
     * что и {@link #renderLitBody}.
     */
    public void renderAtmosphereBody(PoseStack poseStack,
                                     BodyParams body,
                                     float size, float atmosphereScale, int layers,
                                     float r, float g, float b, float alpha, float brightness,
                                     float timeOfDay,
                                     @Nullable LightParams light) {
        Vector3f lightDir = lightDirection(light, timeOfDay);
        Matrix4f orientation = bodyOrientation(body);
        float[] br = computeBrightness(orientation, lightDir);
        Vector3f dir = bodyDirection(body, timeOfDay);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        float step = 0.05F;

        for (int i = layers - 1; i >= 0; i--) {
            float layerScale = size * (atmosphereScale + i * step);
            float layerAlpha = alpha * (1.0F - (float) i / layers) / layers;

            poseStack.pushPose();
            poseStack.translate(dir.x * body.distance, dir.y * body.distance, dir.z * body.distance);
            poseStack.scale(layerScale, layerScale, layerScale);
            poseStack.mulPose(Axis.XP.rotationDegrees(body.tiltX));
            poseStack.mulPose(Axis.ZP.rotationDegrees(body.tiltZ));
            poseStack.mulPose(Axis.YP.rotationDegrees(body.spinDeg));

            Matrix4f mat = poseStack.last().pose();
            BufferBuilder buf = Tesselator.getInstance().getBuilder();
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            atmQuad(buf, mat, -1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1, 1, r, g, b, br[0] * brightness * layerAlpha);
            atmQuad(buf, mat, 1, -1, -1, -1, -1, -1, -1, 1, -1, 1, 1, -1, r, g, b, br[1] * brightness * layerAlpha);
            atmQuad(buf, mat, 1, -1, 1, 1, -1, -1, 1, 1, -1, 1, 1, 1, r, g, b, br[2] * brightness * layerAlpha);
            atmQuad(buf, mat, -1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1, r, g, b, br[3] * brightness * layerAlpha);
            atmQuad(buf, mat, -1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, -1, r, g, b, br[4] * brightness * layerAlpha);
            atmQuad(buf, mat, -1, -1, -1, 1, -1, -1, 1, -1, 1, -1, -1, 1, r, g, b, br[5] * brightness * layerAlpha);

            BufferUploader.drawWithShader(buf.end());
            poseStack.popPose();
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    // =====================================================================

    /** Helper: emit one colored quad (no UV) for the atmosphere shell. */
    private static void atmQuad(BufferBuilder buf, Matrix4f mat,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float r, float g, float b, float a) {
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a).endVertex();
        buf.vertex(mat, x3, y3, z3).color(r, g, b, a).endVertex();
        buf.vertex(mat, x4, y4, z4).color(r, g, b, a).endVertex();
    }

    /**
     * Renders a radial star-like aura (glow disc) around the cube position.
     * Uses a TRIANGLE_FAN disc with additive blending — brightness fades from center
     * to edge, producing a soft, uniform halo reminiscent of starlight.
     * Unlike renderAtmosphere, the aura is NOT affected by sun direction:
     * it glows equally in all directions like a star. Multiple layers with
     * increasing size and decreasing alpha create a soft, blooming halo.
     * <p>
     * Call AFTER renderLitCube / renderCube so the aura composites on top.
     *
     * @param auraSize          radius of the innermost glow disc in world units
     *                          (should be larger than the cube's scale)
     * @param r, g, b           aura color (0.0–1.0)
     * @param brightness        peak brightness multiplier at the center (0.0–1.0+)
     * @param alpha             maximum opacity (0.0–1.0)
     * @param layers            number of stacked glow shells (more = softer halo;
     *                          3–5 is a good range)
     * @param orbitPhaseOffset  orbit offset in day-fractions (0 = no orbit;
     *                          non-zero enables sun-like orbit)
     * @param orbitInclination  tilt of the orbit path in degrees
     */
    public void renderAura(PoseStack poseStack,
                           float x, float y, float z,
                           float auraSize,
                           float r, float g, float b,
                           float brightness, float alpha, int layers,
                           float timeOfDay,
                           float orbitPhaseOffset, float orbitInclination) {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int segments = 16;

        for (int layer = 0; layer < layers; layer++) {
            float layerScale = auraSize * (1.0F + layer * 0.2F);
            float layerAlpha = alpha * (1.0F - (float) layer / layers);
            float layerBrightness = brightness * (1.0F - (float) layer / (layers * 2));

            poseStack.pushPose();
            applyOrbitRotations(poseStack, timeOfDay, orbitPhaseOffset, orbitInclination);
            poseStack.translate(x, y, z);

            Matrix4f mat = poseStack.last().pose();
            BufferBuilder buf = Tesselator.getInstance().getBuilder();
            buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

            // Center vertex — brightest
            buf.vertex(mat, 0.0F, 0.0F, 0.0F)
                .color(r * layerBrightness, g * layerBrightness, b * layerBrightness, layerAlpha)
                .endVertex();

            // Edge vertices — fade to transparent
            for (int i = 0; i <= segments; i++) {
                float angle = (float) i / segments * Mth.TWO_PI;
                float ex = Mth.cos(angle) * layerScale;
                float ez = Mth.sin(angle) * layerScale;
                buf.vertex(mat, ex, 0.0F, ez)
                    .color(r * layerBrightness, g * layerBrightness, b * layerBrightness, 0.0F)
                    .endVertex();
            }

            BufferUploader.drawWithShader(buf.end());
            poseStack.popPose();
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public void renderStarRays(PoseStack poseStack,
                               float x, float y, float z,
                               float r, float g, float b,
                               float[] rayLengths, // Массив из 4 элементов: длина каждого луча
                               float baseWidth,    // Ширина луча у основания (возле центра звезды)
                               float maxAlpha,     // Максимальная прозрачность у основания
                               float rotationOffset, float timeOfDay,
                               float orbitPhaseOffset, float orbitInclination) { // Общий угол поворота всех 4 лучей (в радианах)

        if (rayLengths == null || rayLengths.length < 4) {
            return; // Защита от ошибок
        }

        RenderSystem.enableBlend();
        // Аддитивный блендинг для эффекта свечения
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Получаем вращение камеры, чтобы лучи всегда смотрели на игрока (биллбординг)
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        float yaw = camera.getYRot();
        float pitch = camera.getXRot();
        Quaternionf billboardRotation = new Quaternionf()
                .rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(Math.min(pitch, -20.0F)));

        Quaternionf cameraRotation = camera.rotation();

        poseStack.pushPose();
        applyOrbitRotations(poseStack, timeOfDay, orbitPhaseOffset, orbitInclination);
        poseStack.translate(x, y, z);
        //poseStack.mulPose(cameraRotation); // Поворачиваем всю систему координат к камере
        poseStack.mulPose(billboardRotation);

        Matrix4f mat = poseStack.last().pose();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // Рисуем 4 луча
        for (int i = 0; i < 4; i++) {
            float length = rayLengths[i];
            if (length <= 0.0F) continue;

            // Угол текущего луча (каждые 90 градусов = PI/2) + общий поворот
            float angle = (i * Mth.HALF_PI) + rotationOffset;

            // Вектор направления луча
            float dx = Mth.cos(angle);
            float dy = Mth.sin(angle);

            // Перпендикулярный вектор для задания ширины луча
            float px = -Mth.sin(angle);
            float py = Mth.cos(angle);

            // Вершина 1: Центр звезды (общая для всех лучей, но дублируем для простоты TRIANGLES)
            buf.vertex(mat, 0.0F, 0.0F, 0.0F)
                    .color(r, g, b, maxAlpha)
                    .endVertex();

            // Вершина 2: Правый край кончика луча (ширина сужается к концу, делаем tipWidth = baseWidth * 0.1F)
            float tipWidth = baseWidth * 0.1F;
            float x2 = (dx * length) + (px * tipWidth);
            float y2 = (dy * length) + (py * tipWidth);
            buf.vertex(mat, x2, y2, 0.0F)
                    .color(r, g, b, 0.0F) // Прозрачность 0 на кончике
                    .endVertex();

            // Вершина 3: Левый край кончика луча
            float x3 = (dx * length) - (px * tipWidth);
            float y3 = (dy * length) - (py * tipWidth);
            buf.vertex(mat, x3, y3, 0.0F)
                    .color(r, g, b, 0.0F) // Прозрачность 0 на кончике
                    .endVertex();
        }

        BufferUploader.drawWithShader(buf.end());
        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Computes per-face Lambertian brightness by transforming the sun direction
     * into the cube's local frame via the inverse of the full rotation chain.
     * Returns float[6]: +Z, -Z, +X, -X, +Y, -Y.
     */
    private static float[] computeFaceBrightness(float timeOfDay, float elevationDeg, float azimuthDeg,
                                                  float rotX, float rotY, float rotZ,
                                                  float orbitPhaseOffset, float orbitInclination) {
        Vector3f sunDir = sunDirection(timeOfDay);

        // Build the exact same rotation matrix as the PoseStack chain,
        // then invert it to transform sun direction into cube's local frame.
        Matrix4f m = new Matrix4f();
        if (orbitPhaseOffset != 0.0F) {
            m.rotateY(-90.0F * Mth.DEG_TO_RAD);
            if (orbitInclination != 0.0F) {
                m.rotateZ(orbitInclination * Mth.DEG_TO_RAD);
            }
            m.rotateX(((timeOfDay + orbitPhaseOffset) * 360.0F) * Mth.DEG_TO_RAD);
        }
        m.rotateZ(elevationDeg * Mth.DEG_TO_RAD);
        m.rotateY(azimuthDeg * Mth.DEG_TO_RAD);
        m.rotateX(rotX * Mth.DEG_TO_RAD);
        m.rotateY(rotY * Mth.DEG_TO_RAD);
        m.rotateZ(rotZ * Mth.DEG_TO_RAD);
        m.invert();
        m.transformDirection(sunDir);
        sunDir.normalize();

        return new float[]{
            faceBrightness( 0,  0,  1, sunDir),  // +Z
            faceBrightness( 0,  0, -1, sunDir),  // -Z
            faceBrightness( 1,  0,  0, sunDir),  // +X
            faceBrightness(-1,  0,  0, sunDir),  // -X
            faceBrightness( 0,  1,  0, sunDir),  // +Y
            faceBrightness( 0, -1,  0, sunDir),  // -Y
        };
    }

    /** Applies sun-like orbit rotations: YP(-90°), optional ZP(inclination), then XP(orbitAngle). */
    private static void applyOrbitRotations(PoseStack poseStack, float timeOfDay, float phaseOffset, float inclination) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        if (inclination != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(inclination));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees((timeOfDay + phaseOffset) * 360.0F));
    }

    /** Applies translate + scale + rotX + rotY + rotZ to the pose stack. */
    private static void applyCubeTransforms(PoseStack poseStack,
                                              float x, float y, float z, float scale,
                                              float rotX, float rotY, float rotZ) {
        poseStack.translate(x, y, z);
        if (scale != 1.0F) poseStack.scale(scale, scale, scale);
        if (rotX != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        if (rotY != 0.0F) poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        if (rotZ != 0.0F) poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
    }

    /** Sun direction in camera frame from time-of-day (matches vanilla sun orbit). */
    private static Vector3f sunDirection(float timeOfDay) {
        float angle = timeOfDay * Mth.TWO_PI;
        return new Vector3f(-Mth.sin(angle), Mth.cos(angle), 0.0F);
    }

    /**
     * Renders a textured sky sprite (flat quad) for distant objects like galaxies, nebulae, etc.
     * Supports two positioning modes:
     * <ul>
     *   <li><b>Fixed</b> (orbit=false): placed at the given elevation + azimuth.</li>
     *   <li><b>Orbit</b> (orbit=true): orbits across the sky like the sun, with an optional
     *       phase offset (0.0 = same as sun, 0.5 = opposite side of sky).</li>
     * </ul>
     *
     * @param texture       sprite texture ResourceLocation
     * @param size          half-extent of the quad in world units (e.g. 30F)
     * @param distance      distance from camera (e.g. 100F)
     * @param brightness    brightness multiplier (0.0–1.0), applied to all color channels
     * @param alpha         opacity (0.0–1.0)
     * @param orbit         true = orbit like the sun, false = fixed position
     * @param phaseOffset   orbit phase offset in day-fractions (only when orbit=true)
     * @param orbitInclination tilt of the orbit path in degrees (only when orbit=true)
     * @param elevationDeg  elevation angle in degrees (only when orbit=false)
     * @param azimuthDeg    azimuth angle in degrees (only when orbit=false)
     * @param selfRotation  self-rotation angle in degrees (slow spin of the quad)
     * @param timeOfDay     level.getTimeOfDay(partialTicks) — drives orbit position
     */
    public void renderSkySprite(PoseStack poseStack,
                                ResourceLocation texture,
                                float size, float distance,
                                float brightness, float alpha,
                                boolean orbit, float phaseOffset, float orbitInclination,
                                float elevationDeg, float azimuthDeg,
                                float selfRotation,
                                float timeOfDay) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(brightness, brightness, brightness, alpha);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.depthMask(false);

        poseStack.pushPose();

        if (orbit) {
            // Sun-like orbit with optional phase offset and inclination
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            if (orbitInclination != 0.0F) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(orbitInclination));
            }
            float orbitAngle = (timeOfDay + phaseOffset) * 360.0F;
            poseStack.mulPose(Axis.XP.rotationDegrees(orbitAngle));
        } else {
            // Fixed position in the sky
            poseStack.mulPose(Axis.ZP.rotationDegrees(elevationDeg));
            poseStack.mulPose(Axis.YP.rotationDegrees(azimuthDeg));
        }

        // Self-rotation around the view axis
        if (selfRotation != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(selfRotation));
        }

        poseStack.translate(0.0F, distance, 0.0F);
        poseStack.scale(size, size, size);

        Matrix4f mat = poseStack.last().pose();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        // CW-wound quad, facing -Y (toward camera at origin)
        buf.vertex(mat, -1.0F, 0.0F,  1.0F).uv(0, 1).endVertex();
        buf.vertex(mat,  1.0F, 0.0F,  1.0F).uv(1, 1).endVertex();
        buf.vertex(mat,  1.0F, 0.0F, -1.0F).uv(1, 0).endVertex();
        buf.vertex(mat, -1.0F, 0.0F, -1.0F).uv(0, 0).endVertex();

        BufferUploader.drawWithShader(buf.end());
        poseStack.popPose();

        // Restore render state
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static BufferBuilder.RenderedBuffer buildSkyDisc(BufferBuilder p_234268_, float p_234269_) {
        float f = Math.signum(p_234269_) * 512.0F;
        float f1 = 512.0F;
        RenderSystem.setShader(GameRenderer::getPositionShader);
        p_234268_.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        p_234268_.vertex(0.0D, (double)p_234269_, 0.0D).endVertex();

        for(int i = -180; i <= 180; i += 45) {
            p_234268_.vertex((double)(f * Mth.cos((float)i * ((float)Math.PI / 180F))), (double)p_234269_, (double)(512.0F * Mth.sin((float)i * ((float)Math.PI / 180F)))).endVertex();
        }

        return p_234268_.end();
    }
}
