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

import javax.annotation.Nullable;

public class SkyHelper {

    @Nullable
    private VertexBuffer skyBuffer;

    @Nullable
    private VertexBuffer darkBuffer;

    public VertexBuffer getSkyBuffer() {
        return this.skyBuffer;
    }

    public VertexBuffer getDarkBuffer() {
        return this.darkBuffer;
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

    public void renderSky(VertexBuffer starBuffer, Matrix4f matrix, PoseStack poseStack, Minecraft minecraft, ResourceLocation sunTexture, float partialTicks, float sunBrightness, float starBrightness, Runnable setupFog) {
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
        poseStack.mulPose(Axis.XP.rotationDegrees(minecraft.level.getTimeOfDay(partialTicks) * 360.0F));
        Matrix4f matrix4f1 = poseStack.last().pose();
        float f12 = 30.0F;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, sunTexture);
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix4f1, -f12, 100.0F, -f12).uv(0.0F, 0.0F).endVertex();
        bufferbuilder.vertex(matrix4f1, f12, 100.0F, -f12).uv(1.0F, 0.0F).endVertex();
        bufferbuilder.vertex(matrix4f1, f12, 100.0F, f12).uv(1.0F, 1.0F).endVertex();
        bufferbuilder.vertex(matrix4f1, -f12, 100.0F, f12).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
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
