package stellarstory.client.renderer.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import stellarstory.api.CelestialRegisters;
import stellarstory.common.registers.StellarDimensions;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class CelestialSky extends DimensionSpecialEffects {

    private static final ResourceLocation SUN_LOCATION = new ResourceLocation("textures/environment/sun.png");

    //private final VertexBuffer starBuffer = StarHelper.createStars(0.1F, 190, 160, -1);
    private final Minecraft minecraft = Minecraft.getInstance();
    @Nullable
    protected VertexBuffer starBuffer;

    public CelestialSky() {
        super(Float.NaN, false, SkyType.NONE, false, false);

        this.createStars();
    }

    private void createStars() {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        if (this.starBuffer != null) {
            this.starBuffer.close();
        }

        this.starBuffer = new VertexBuffer();
        BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer = this.drawStars(bufferbuilder);
        this.starBuffer.bind();
        this.starBuffer.upload(bufferbuilder$renderedbuffer);
        VertexBuffer.unbind();
    }

    private BufferBuilder.RenderedBuffer drawStars(BufferBuilder builder) {
        RandomSource randomsource = RandomSource.create(10842L);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        for(int i = 0; i < 1500; ++i) {
            double d0 = (double)(randomsource.nextFloat() * 2.0F - 1.0F);
            double d1 = (double)(randomsource.nextFloat() * 2.0F - 1.0F);
            double d2 = (double)(randomsource.nextFloat() * 2.0F - 1.0F);
            double d3 = (double)(0.15F + randomsource.nextFloat() * 0.1F);
            double d4 = d0 * d0 + d1 * d1 + d2 * d2;
            if (d4 < 1.0D && d4 > 0.01D) {
                d4 = 1.0D / Math.sqrt(d4);
                d0 *= d4;
                d1 *= d4;
                d2 *= d4;
                double d5 = d0 * 100.0D;
                double d6 = d1 * 100.0D;
                double d7 = d2 * 100.0D;
                double d8 = Math.atan2(d0, d2);
                double d9 = Math.sin(d8);
                double d10 = Math.cos(d8);
                double d11 = Math.atan2(Math.sqrt(d0 * d0 + d2 * d2), d1);
                double d12 = Math.sin(d11);
                double d13 = Math.cos(d11);
                double d14 = randomsource.nextDouble() * Math.PI * 2.0D;
                double d15 = Math.sin(d14);
                double d16 = Math.cos(d14);

                for(int j = 0; j < 4; ++j) {
                    double d17 = 0.0D;
                    double d18 = (double)((j & 2) - 1) * d3;
                    double d19 = (double)((j + 1 & 2) - 1) * d3;
                    double d20 = 0.0D;
                    double d21 = d18 * d16 - d19 * d15;
                    double d22 = d19 * d16 + d18 * d15;
                    double d23 = d21 * d12 + 0.0D * d13;
                    double d24 = 0.0D * d12 - d21 * d13;
                    double d25 = d24 * d9 - d22 * d10;
                    double d26 = d22 * d9 + d24 * d10;
                    builder.vertex(d5 + d25, d6 + d23, d7 + d26).endVertex();
                }
            }
        }

        return builder.end();
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTicks, PoseStack poseStack, Camera camera,
                             Matrix4f matrix, boolean isFoggy, Runnable setupFog) {

        setupFog.run();
        if (!isFoggy) {
            FogType fogtype = camera.getFluidInCamera();
            if (fogtype != FogType.POWDER_SNOW && fogtype != FogType.LAVA
                   && !minecraft.levelRenderer.doesMobEffectBlockSky(camera))
            {
                RenderSystem.disableTexture();
                /** SKY RENDER */
                Vec3 vec3 = level.getSkyColor(minecraft.gameRenderer.getMainCamera().getPosition(), partialTicks);
                float r = 0;//(float) vec3.x;
                float g = 0;//(float) vec3.y;
                float b = 0;//(float) vec3.z;

                FogRenderer.levelFogColor();

                BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
                ShaderInstance shaderinstance = RenderSystem.getShader();


                RenderSystem.depthMask(false);
                RenderSystem.setShaderColor(r, g, b, 1.0F);
                minecraft.levelRenderer.skyBuffer.bind();
                minecraft.levelRenderer.skyBuffer.drawWithShader(poseStack.last().pose(), matrix, shaderinstance);
                VertexBuffer.unbind();

                //RenderSystem.defaultBlendFunc();
                /** SUNRISE COLOR */

                /** RENDER SUN */

                float rainLevel = 1.0F;// - level.getRainLevel(partialTicks);
                RenderSystem.depthMask(false);

                poseStack.pushPose();
                poseStack.mulPose(Vector3f.XP.rotationDegrees(level.getTimeOfDay(partialTicks) * 360.0F));

                //other bodies
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                RenderSystem.disableTexture();

                /** STARS */

                float starBrightness = level.getStarBrightness(partialTicks) * rainLevel;
                if (starBrightness > 0.0F) {
                    RenderSystem.setShaderColor(starBrightness, starBrightness, starBrightness, starBrightness);
                    FogRenderer.setupNoFog();
                    this.starBuffer.bind();
                    this.starBuffer.drawWithShader(poseStack.last().pose(), matrix, GameRenderer.getPositionShader());
                    VertexBuffer.unbind();
                    setupFog.run();
                }
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
                poseStack.popPose();

                /** SUN */
                poseStack.pushPose();
                RenderSystem.enableTexture();
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

                RenderSystem.enableCull();
                Matrix4f matrix4f1 = poseStack.last().pose();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//                poseStack.translate(0.0F, -camera.getPosition().y, 0.0F);

                //poseStack.mulPose(Vector3f.YP.rotationDegrees(-90F));
                //poseStack.mulPose(Vector3f.XP.rotationDegrees((level.getTimeOfDay(partialTicks) * 360.0F) * 45F));
                float size = 10F;
                Matrix4f finalMatrix = matrix4f1.copy();
                float offsetY = 10F;


                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, StellarDimensions.sol.getProperties().icon);
                bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                /*bufferbuilder.vertex(matrix4f1, -size, 100.0F, -size).uv(0.0F, 0.0F).endVertex();
                bufferbuilder.vertex(matrix4f1, size, 100.0F, -size).uv(1.0F, 0.0F).endVertex();
                bufferbuilder.vertex(matrix4f1, size, 100.0F, size).uv(1.0F, 1.0F).endVertex();
                bufferbuilder.vertex(matrix4f1, -size, 100.0F, size).uv(0.0F, 1.0F).endVertex();
                */

                // Грань 1: Front face (Z = +size)
                bufferbuilder.vertex(finalMatrix, -size, -size + offsetY, size).uv(0.0F, 0.0F).endVertex(); // Левый нижний угол
                bufferbuilder.vertex(finalMatrix, size, -size + offsetY, size).uv(1.0F, 0.0F).endVertex(); // Правый нижний угол
                bufferbuilder.vertex(finalMatrix, size, size + offsetY, size).uv(1.0F, 1.0F).endVertex(); // Правый верхний угол
                bufferbuilder.vertex(finalMatrix, -size, size + offsetY, size).uv(0.0F, 1.0F).endVertex(); // Левый верхний угол

                // Грань 2: Back face (Z = -size)
                bufferbuilder.vertex(finalMatrix, -size, -size + offsetY, -size).uv(0.0F, 0.0F).endVertex(); // Левый нижний угол
                bufferbuilder.vertex(finalMatrix, -size, size + offsetY, -size).uv(1.0F, 0.0F).endVertex(); // Левый верхний угол
                bufferbuilder.vertex(finalMatrix, size, size + offsetY, -size).uv(1.0F, 1.0F).endVertex(); // Правый верхний угол
                bufferbuilder.vertex(finalMatrix, size, -size + offsetY, -size).uv(0.0F, 1.0F).endVertex(); // Правый нижний угол

                // Грань 3: Top face (Y = +size)
                bufferbuilder.vertex(finalMatrix, -size, size + offsetY, -size).uv(0.0F, 0.0F).endVertex(); // Левый передний угол
                bufferbuilder.vertex(finalMatrix, -size, size + offsetY, size).uv(1.0F, 0.0F).endVertex(); // Левый задний угол
                bufferbuilder.vertex(finalMatrix, size, size + offsetY, size).uv(1.0F, 1.0F).endVertex(); // Правый задний угол
                bufferbuilder.vertex(finalMatrix, size, size + offsetY, -size).uv(0.0F, 1.0F).endVertex(); // Правый передний угол

                // Грань 4: Bottom face (Y = -size)
                bufferbuilder.vertex(finalMatrix, -size, -size + offsetY, -size).uv(0.0F, 0.0F).endVertex(); // Левый передний угол
                bufferbuilder.vertex(finalMatrix, size, -size + offsetY, -size).uv(1.0F, 0.0F).endVertex(); // Правый передний угол
                bufferbuilder.vertex(finalMatrix, size, -size + offsetY, size).uv(1.0F, 1.0F).endVertex(); // Правый задний угол
                bufferbuilder.vertex(finalMatrix, -size, -size + offsetY, size).uv(0.0F, 1.0F).endVertex(); // Левый задний угол

                // !!!Грань 5: Right face (X = +size)
                bufferbuilder.vertex(finalMatrix, -size, -size + offsetY, -size).uv(0.0F, 0.0F).endVertex(); // Нижний передний угол
                bufferbuilder.vertex(finalMatrix, -size, -size + offsetY, size).uv(1.0F, 0.0F).endVertex(); // Нижний задний угол
                bufferbuilder.vertex(finalMatrix, -size, size + offsetY, size).uv(1.0F, 1.0F).endVertex(); // Верхний задний угол
                bufferbuilder.vertex(finalMatrix, -size, size + offsetY, -size).uv(0.0F, 1.0F).endVertex(); // Верхний передний угол

                // Грань 6: Left face (X = -size)
                bufferbuilder.vertex(finalMatrix, size, -size + offsetY, -size).uv(0.0F, 0.0F).endVertex(); // Нижний передний угол
                bufferbuilder.vertex(finalMatrix, size, size + offsetY, -size).uv(1.0F, 0.0F).endVertex(); // Верхний передний угол
                bufferbuilder.vertex(finalMatrix, size, size + offsetY, size).uv(1.0F, 1.0F).endVertex(); // Верхний задний угол
                bufferbuilder.vertex(finalMatrix, size, -size + offsetY, size).uv(0.0F, 1.0F).endVertex(); // Нижний задний угол

                BufferUploader.drawWithShader(bufferbuilder.end());
                poseStack.popPose();

              /*  poseStack.pushPose();
                this.renderPlanet(poseStack, bufferbuilder, camera, ticks, partialTicks, level.getTimeOfDay(partialTicks));
                poseStack.popPose();*/

                /**DARK SKY RENDER */
                drawDarkSky(minecraft, camera, poseStack, matrix, shaderinstance, partialTicks);

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.depthMask(true);
            }
        }
        return true;
    }

    private static void drawDarkSky(Minecraft mc, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix,
                                   ShaderInstance shaderInstance, float partialTick) {
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        double d0 = camera.getEntity().getEyePosition(partialTick).y - mc.level.getLevelData().getHorizonHeight(mc.level);
        if (d0 < 0.0D) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 12.0D, 0.0D);
            mc.levelRenderer.darkBuffer.bind();
            mc.levelRenderer.darkBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shaderInstance);
            VertexBuffer.unbind();
            poseStack.popPose();
        }
    }
//
//    public static void renderCube(PoseStack poseStack, MultiBufferSource bufferSource, float x, float y, float z, float scale) {
//        Minecraft minecraft = Minecraft.getInstance();
//
//        RenderSystem.enableTexture();
//
//        RenderSystem.setShader(GameRenderer::getPositionTexShader);
//        RenderSystem.setShaderTexture(0, StellarDimensions.sol.getIcon());
//
//        VertexConsumer buffer = bufferSource.getBuffer(RenderType.text(StellarDimensions.sol.getIcon()));
//        Matrix4f matrix = poseStack.last().pose();
//
//        BufferBuilder builder = Tesselator.getInstance().getBuilder();
//        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
//        // Верхняя грань
//        addQuad(builder, matrix, 0, 1, 1, 0, 0,
//                1, 1, 1, 1, 0,
//                1, 1, 0, 1, 1,
//                0, 1, 0, 0, 1,
//                0, 0, 0);
//        //низ
//        addQuad(builder, matrix, 0, 0, 1, 0, 1,
//                0, 0, 0, 0, 0,
//                1, 0, 0, 1, 0,
//                1, 0, 1, 1, 1,
//                0, 0, 0);
//        //перед
//        addQuad(builder, matrix, 0, 0, 1, 0, 0,
//                1, 0, 1, 1, 0,
//                1, 1, 1, 1, 1,
//                0, 1, 1, 0, 1,
//                0, 0, 0);
//        //зад
//        addQuad(builder, matrix, 1, 0, 0, 0, 0,
//                0, 0, 0, 1, 0,
//                0, 1, 0, 1, 1,
//                1, 1, 0, 0, 1,
//                0, 0, 1);
//        //прав
//        addQuad(builder, matrix, 0, 0, 0, 0, 0,
//                0, 0, 1, 1, 0,
//                0, 1, 1, 1, 1,
//                0, 1, 0, 0, 1,
//                0, 0, 0);
//        //лев
//        addQuad(builder, matrix, 1, 1, 1, 0, 0,
//                1, 0, 1, 1, 0,
//                1, 0, 0, 1, 1,
//                1, 1, 0, 0, 1,
//                0, 0, 0);
//
//        BufferUploader.drawWithShader(builder.end());
//
//        RenderSystem.disableTexture();
//
//    }

    private static void addQuad(BufferBuilder buffer, Matrix4f matrix,
                                float x1, float y1, float z1, float u1, float v1,
                                float x2, float y2, float z2, float u2, float v2,
                                float x3, float y3, float z3, float u3, float v3,
                                float x4, float y4, float z4, float u4, float v4,
                                float nx, float ny, float nz) {


        buffer.vertex(matrix, x1, y1, z1).color(1.0F, 1.0F, 1.0F, 1.0F).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(nx, ny, nz).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(1.0F, 1.0F, 1.0F, 1.0F).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(nx, ny, nz).endVertex();
        buffer.vertex(matrix, x3, y3, z3).color(1.0F, 1.0F, 1.0F, 1.0F).uv(u3, v3).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(nx, ny, nz).endVertex();
        buffer.vertex(matrix, x4, y4, z4).color(1.0F, 1.0F, 1.0F, 1.0F).uv(u4, v4).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(nx, ny, nz).endVertex();


    }

    private void renderPlanet(PoseStack poseStack, BufferBuilder buffer, Camera camera, int ticks, float partialTicks, float timeOfDay) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        float sunAngle = timeOfDay * 360.0F; // Положение солнца в градусах
        float rotationAngle = ticks * 0.1F; // Скорость вращения планеты
        float planetX = -90.0F;
        float planetY = rotationAngle; // Высота над горизонтом
        float planetZ = 0.0F;

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        //poseStack.pushPose();
        poseStack.translate(0, 0, 0);


        // Вращение куба вокруг своей оси
        poseStack.mulPose(Vector3f.XP.rotationDegrees(0));

        // Масштабирование планеты
        poseStack.scale(50.0F, 50.0F, 50.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, StellarDimensions.sol.getProperties().icon);
        // Отрисовка всех граней куба
        drawCubeWithShadows(poseStack, buffer, ticks, partialTicks, sunAngle);

       // poseStack.popPose();

    }

    private void drawCubeWithShadows(PoseStack poseStack, BufferBuilder buffer, int ticks, float partialTicks, float sunAngle) {



        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        // Верхняя грань
        drawQuadWithShadow(buffer, poseStack, 0, 1, 1, 0, 0,
                1, 1, 1, 1, 0,
                1, 1, 0, 1, 1,
                0, 1, 0, 0, 1,
                sunAngle, false);

        // Нижняя грань
        drawQuadWithShadow(buffer, poseStack, 0, 0, 1, 0, 1,
                0, 0, 0, 0, 0,
                1, 0, 0, 1, 0,
                1, 0, 1, 1, 1,
                sunAngle, true);


        // Передняя грань
        drawQuadWithShadow(buffer, poseStack, 0, 0, 1, 0, 0,
                1, 0, 1, 1, 0,
                1, 1, 1, 1, 1,
                0, 1, 1, 0, 1,
                sunAngle, false);

        // Задняя грань
        drawQuadWithShadow(buffer, poseStack, 1, 0, 0, 0, 0,
                0, 0, 0, 1, 0,
                0, 1, 0, 1, 1,
                1, 1, 0, 0, 1,
                sunAngle, true);

        // Правая грань
        drawQuadWithShadow(buffer, poseStack, 0, 0, 0, 0, 0,
                0, 0, 1, 1, 0,
                0, 1, 1, 1, 1,
                0, 1, 0, 0, 1,
                sunAngle, false);

        // Левая грань
        drawQuadWithShadow(buffer, poseStack, 1, 1, 1, 0, 0,
                1, 0, 1, 1, 0,
                1, 0, 0, 1, 1,
                1, 1, 0, 0, 1,
                sunAngle, true);

        BufferUploader.drawWithShader(buffer.end());
    }

    private void drawQuadWithShadow(VertexConsumer buffer, PoseStack poseStack, float x1, float y1, float z1, float u1, float v1,
                                    float x2, float y2, float z2, float u2, float v2,
                                    float x3, float y3, float z3, float u3, float v3,
                                    float x4, float y4, float z4, float u4, float v4, float sunAngle, boolean isDarkSide) {
        float lightValue = 1.0F;// calculateLightValue(sunAngle, (x1 + x2) / 2, (y1 + y2) / 2, (z1 + z2) / 2, isDarkSide);

        // Вершины квадрата в 3D пространстве
        buffer.vertex(poseStack.last().pose(), x1, y1, z1).uv(u1, v1).color(lightValue, lightValue, lightValue, 1.0F).endVertex();
        buffer.vertex(poseStack.last().pose(), x2, y2, z2).uv(u2, v2).color(lightValue, lightValue, lightValue, 1.0F).endVertex();
        buffer.vertex(poseStack.last().pose(), x3, y3, z3).uv(u3, v3).color(lightValue, lightValue, lightValue, 1.0F).endVertex();
        buffer.vertex(poseStack.last().pose(), x4, y4, z4).uv(u4, v4).color(lightValue, lightValue, lightValue, 1.0F).endVertex();

    }


    private float[] getVertex(float x1, float y1, float x2, float y2, float z1, float z2, float u1, float v1, float u2, float v2, int index) {
        float[] vertices = {
                x1, y1, z1, u1, v1,
                x2, y1, z2, u2, v1,
                x2, y2, z2, u2, v2,
                x1, y2, z1, u1, v2
        };
        return new float[]{vertices[index * 5], vertices[index * 5 + 1], vertices[index * 5 + 2], vertices[index * 5 + 3], vertices[index * 5 + 4]};
    }

    private float calculateLightValue(float sunAngle, float x, float y, float z, boolean isDarkSide) {
        float angleRad = (float) Math.toRadians(sunAngle);
        Vector3f lightDirection = new Vector3f(
                (float) Math.cos(angleRad),
                (float) Math.sin(angleRad),
                0.0F
        );

        // Нормализуем направление света
        lightDirection.normalize();

        // Расчет нормали к грани куба
        Vector3f normal = new Vector3f(x, y, z);
        normal.normalize();

        // Расчет интенсивности света на основе скалярного произведения направления света и нормали
        float lightIntensity = lightDirection.dot(normal);
        lightIntensity = Math.max(0.1F, Math.min(1.0F, lightIntensity)); // Ограничиваем интенсивность света от 0.1 до 1.0
        if (isDarkSide) {
            lightIntensity *= 0.5F; // Темная сторона получает меньше света
        }
        return lightIntensity;
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

    /** Бесполезный мусор?, неправильно переданы аргументы. Вместо z - y*/
    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

}
