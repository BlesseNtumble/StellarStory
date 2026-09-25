package stellarstory.client.renderer.sky.helper;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class SkyHelper {

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

    public void renderSky(VertexBuffer starBuffer, Matrix4f matrix, PoseStack poseStack, Minecraft minecraft, ResourceLocation sunTexture, float partialTicks, float sunBrightness, float starBrightness, float orbitInclination, Runnable setupFog) {
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
                           float scale, float alpha, float timeOfDay,
                           float orbitPhaseOffset, float orbitInclination,
                           ResourceLocation... textures) {
        boolean multiTex = textures.length >= 6;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

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
     * Renders a textured cube with per-face Lambertian shading based on sun direction.
     * Supports either 1 texture (applied to all faces) or 6 textures (one per face:
     * +Z, -Z, +X, -X, +Y, -Y). Uses immediate-mode POSITION_TEX_COLOR.
     *
     * x, y, z	Позиция центра в системе координат неба (камера в 0,0,0; юниты условные, 100 ≈ «далеко», 1 ≈ «на голове»)	translate до scale, так что масштаб радиус не сдвигает
     * rotX, rotY, rotZ	Наклон оси + самоповорот куба вокруг своих осей (градусы)	rotY обычно и есть суточное вращение; rotX/rotZ — наклон оси
     * scale	Радиус куба	По сути угловой размер: asin(scale / расстояние)
     * alpha	Непрозрачность граней	1.0F — нормально, у куба с текстурой обычно и не трогают
     * timeOfDay	level.getTimeOfDay(partialTicks)	Управляет и орбитой, и направлением света
     * elevationDeg	❗ Только раскраска: сдвигает терминатор	Позицию не меняет
     * azimuthDeg	❗ Только раскраска: поворот линии света	Позицию не меняет
     * orbitPhaseOffset	Фаза орбиты в долях суток (0.5 = противоположно солнцу)	❗ 0 орбиту не выключает, только сдвигает фазу
     * orbitInclination	Наклон плоскости орбиты в градусах	При phaseOffset == 0 игнорируется и орбитой, и раскраской
     * textures...	1 текстура на все грани или 6	Порядок 6 шт. = +Z, -Z, +X, -X, +Y, -Y (как CUBE_FACES)
     *
     * @param orbitPhaseOffset orbit offset in day-fractions (0 = no orbit; non-zero enables sun-like orbit)
     * @param orbitInclination tilt of the orbit path in degrees relative to the horizon
     * @param textures either 1 texture for all faces, or 6 textures (one per face)
     */
    public void renderLitCube(PoseStack poseStack,
                              float x, float y, float z,
                              float rotX, float rotY, float rotZ,
                              float scale, float alpha,
                              float timeOfDay, float elevationDeg, float azimuthDeg,
                              float orbitPhaseOffset, float orbitInclination,
                              ResourceLocation... textures) {
        float[] b = computeFaceBrightness(timeOfDay, elevationDeg, azimuthDeg, rotX, rotY, rotZ, orbitPhaseOffset, orbitInclination);
        boolean multiTex = textures.length >= 6;

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (!multiTex) {
            RenderSystem.setShaderTexture(0, textures[0]);
        }

        poseStack.pushPose();
        applyOrbitRotations(poseStack, timeOfDay, orbitPhaseOffset, orbitInclination);
        applyCubeTransforms(poseStack, x, y, z, scale, rotX, rotY, rotZ);
        Matrix4f mat = poseStack.last().pose();

        for (int i = 0; i < 6; i++) {
            if (multiTex) {
                RenderSystem.setShaderTexture(0, textures[i]);
            }

            BufferBuilder buf = Tesselator.getInstance().getBuilder();
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            float[] v = CUBE_FACES[i];
            buf.vertex(mat, v[0], v[1], v[2]).uv(0,1).color(b[i], b[i], b[i], alpha).endVertex();
            buf.vertex(mat, v[3], v[4], v[5]).uv(1,1).color(b[i], b[i], b[i], alpha).endVertex();
            buf.vertex(mat, v[6], v[7], v[8]).uv(1,0).color(b[i], b[i], b[i], alpha).endVertex();
            buf.vertex(mat, v[9], v[10],v[11]).uv(0,0).color(b[i], b[i], b[i], alpha).endVertex();

            BufferUploader.drawWithShader(buf.end());
        }

        poseStack.popPose();
    }

    /**
     * Renders a planet that does NOT orbit the sky. Unlike {@link #renderLitCube},
     * elevation/azimuth position the body (they are no longer shading-only), and the
     * axial tilt is applied in world space BEFORE the spin, so the planet keeps its
     * orientation instead of tumbling once per day.
     *
     * @param elevationDeg direction of the body: -90 = straight down, 0 = horizon, +90 = zenith
     * @param azimuthDeg   compass angle, 0 = +Z, 90 = +X (ignored at elevation +/-90)
     * @param distance     distance from the camera; must exceed size or the camera ends up inside
     * @param tiltX, tiltZ axial tilt in degrees, applied before the spin
     * @param spinDeg      self-rotation around the tilted local +Y axis
     * @param size         cube half-extent; angular radius = asin(size / distance)
     * @param sunTime      time of day driving the terminator; offset it to fake a planet phase
     */
    public void renderLitCubeFixed(PoseStack poseStack,
                                   float elevationDeg, float azimuthDeg, float distance,
                                   float tiltX, float tiltZ, float spinDeg,
                                   float size, float alpha,
                                   float sunTime,
                                   ResourceLocation... textures) {
        Matrix4f rotation = fixedRotation(tiltX, tiltZ, spinDeg);
        float[] b = faceBrightnessFor(rotation, sunTime);
        boolean multiTex = textures.length >= 6;

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (!multiTex) {
            RenderSystem.setShaderTexture(0, textures[0]);
        }

        poseStack.pushPose();
        applyFixedPlacement(poseStack, elevationDeg, azimuthDeg, distance, size, tiltX, tiltZ, spinDeg);
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
     * Renders multiple atmospheric layers as a glowing shell around the cube.
     * Uses POSITION_COLOR (no texture), additive blending (SRC_ALPHA, ONE) for a true
     * glow effect, and sun-based per-face brightness. Layers go from outermost (faintest)
     * to innermost (brightest) for correct alpha stacking. Call AFTER renderLitCube.
     *
     * @param atmosphereScale base scale multiplier for the innermost layer (e.g. 1.02F)
     * @param layers         number of atmosphere shells
     * @param r, g, b        atmosphere color (0.0–1.0)
     * @param alpha          total atmosphere opacity (0.0–1.0)
     * @param brightness     atmosphere brightness multiplier, independent from cube shading (e.g. 1.5F for brighter glow)
     */
    public void renderAtmosphere(PoseStack poseStack,
                                 float x, float y, float z,
                                 float rotX, float rotY, float rotZ,
                                 float scale, float atmosphereScale, int layers,
                                 float r, float g, float b, float alpha, float brightness,
                                 float timeOfDay, float elevationDeg, float azimuthDeg,
                                 float orbitPhaseOffset, float orbitInclination) {
        float[] br = computeFaceBrightness(timeOfDay, elevationDeg, azimuthDeg, rotX, rotY, rotZ, orbitPhaseOffset, orbitInclination);

        // Additive blending: atmosphere ADDS brightness → glows brighter than the cube
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        float step = 0.05F;

        // Outer-to-inner so inner (brighter) layers composite on top
        for (int i = layers - 1; i >= 0; i--) {
            float layerScale = scale * (atmosphereScale + i * step);
            float layerAlpha = alpha * (1.0F - (float) i / layers) / layers;

            poseStack.pushPose();
            applyOrbitRotations(poseStack, timeOfDay, orbitPhaseOffset, orbitInclination);
            applyCubeTransforms(poseStack, x, y, z, layerScale, rotX, rotY, rotZ);

            Matrix4f mat = poseStack.last().pose();
            BufferBuilder buf = Tesselator.getInstance().getBuilder();
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            atmQuad(buf, mat, -1,-1, 1,  1,-1, 1,  1, 1, 1, -1, 1, 1, r,g,b, br[0] * brightness * layerAlpha);
            atmQuad(buf, mat,  1,-1,-1, -1,-1,-1, -1, 1,-1,  1, 1,-1, r,g,b, br[1] * brightness * layerAlpha);
            atmQuad(buf, mat,  1,-1, 1,  1,-1,-1,  1, 1,-1,  1, 1, 1, r,g,b, br[2] * brightness * layerAlpha);
            atmQuad(buf, mat, -1,-1,-1, -1,-1, 1, -1, 1, 1, -1, 1,-1, r,g,b, br[3] * brightness * layerAlpha);
            atmQuad(buf, mat, -1, 1, 1,  1, 1, 1,  1, 1,-1, -1, 1,-1, r,g,b, br[4] * brightness * layerAlpha);
            atmQuad(buf, mat, -1,-1,-1,  1,-1,-1,  1,-1, 1, -1,-1, 1, r,g,b, br[5] * brightness * layerAlpha);

            BufferUploader.drawWithShader(buf.end());
            poseStack.popPose();
        }

        // Restore render state
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /** Fixed-placement counterpart of {@link #renderAtmosphere}. Call AFTER renderLitCubeFixed. */
    public void renderAtmosphereFixed(PoseStack poseStack,
                                      float elevationDeg, float azimuthDeg, float distance,
                                      float tiltX, float tiltZ, float spinDeg,
                                      float size, float atmosphereScale, int layers,
                                      float r, float g, float b, float alpha, float brightness,
                                      float sunTime) {
        Matrix4f rotation = fixedRotation(tiltX, tiltZ, spinDeg);
        float[] br = faceBrightnessFor(rotation, sunTime);

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
            applyFixedPlacement(poseStack, elevationDeg, azimuthDeg, distance, layerScale, tiltX, tiltZ, spinDeg);

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

    /** Lambertian brightness for a face given its outward normal and sun direction. */
    private static float faceBrightness(float nx, float ny, float nz, Vector3f sun) {
        float dot = nx * sun.x + ny * sun.y + nz * sun.z;
        return 0.05F + 0.75F * Math.max(0.0F, dot);
    }

    /** Shared Lambert shading: rotates the sun into the cube's local frame via the inverse rotation. */
    private static float[] faceBrightnessFor(Matrix4f rotation, float timeOfDay) {
        Vector3f local = new Vector3f(sunDirection(timeOfDay));
        new Matrix4f(rotation).invert().transformDirection(local);
        local.normalize();

        return new float[]{
                faceBrightness( 0,  0,  1, local),  // +Z
                faceBrightness( 0,  0, -1, local),  // -Z
                faceBrightness( 1,  0,  0, local),  // +X
                faceBrightness(-1,  0,  0, local),  // -X
                faceBrightness( 0,  1,  0, local),  // +Y
                faceBrightness( 0, -1,  0, local),  // -Y
        };
    }

    /** Tilt first, spin second: the spin then runs around the already tilted local +Y axis. */
    private static Matrix4f fixedRotation(float tiltX, float tiltZ, float spinDeg) {
        Matrix4f m = new Matrix4f();
        m.rotateX(tiltX * Mth.DEG_TO_RAD);
        m.rotateZ(tiltZ * Mth.DEG_TO_RAD);
        m.rotateY(spinDeg * Mth.DEG_TO_RAD);
        return m;
    }

    /** Places a unit cube at (elevation, azimuth) * distance, scales it, then applies tilt + spin. */
    private static void applyFixedPlacement(PoseStack poseStack, float elevationDeg, float azimuthDeg,
                                            float distance, float size,
                                            float tiltX, float tiltZ, float spinDeg) {
        float el = elevationDeg * Mth.DEG_TO_RAD;
        float az = azimuthDeg * Mth.DEG_TO_RAD;
        poseStack.translate(Mth.cos(el) * Mth.sin(az) * distance,
                Mth.sin(el) * distance,
                Mth.cos(el) * Mth.cos(az) * distance);
        if (size != 1.0F) poseStack.scale(size, size, size);
        // Порядок совпадает с fixedRotation (Rx * Rz * Ry): спин крутит вокруг локальной Y,
        // наклон применяется уже в мировой системе.
        poseStack.mulPose(Axis.XP.rotationDegrees(tiltX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(tiltZ));
        poseStack.mulPose(Axis.YP.rotationDegrees(spinDeg));
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
