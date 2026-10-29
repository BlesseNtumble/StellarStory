package stellarstory.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import stellarstory.common.blocks.pipe.FluidPipeBlockEntity;

/**
 * Отрисовка жидкости внутри жидкостной трубы.
 *
 * Сама труба остаётся моделью multipart, а BlockEntity рисует только кубик
 * переносимой жидкости в центре сегмента.
 */
public class FluidPipeRenderer implements BlockEntityRenderer<FluidPipeBlockEntity> {

    private static final float CUBE_RADIUS = 0.3125F;

    public FluidPipeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull FluidPipeBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        FluidStack fluid = blockEntity.getRenderedFluid();
        if (fluid.isEmpty()) {
            return;
        }

        int color = fluidColor(fluid);
        float alpha = ((color >>> 24) & 255) / 255.0F;
        if (alpha <= 0.0F) {
            alpha = 0.72F;
        }
        float red = ((color >> 16) & 255) / 255.0F;
        float green = ((color >> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        RenderSystem.disableCull();
        drawCube(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha);
        RenderSystem.enableCull();

        poseStack.popPose();
    }

    private static int fluidColor(FluidStack fluid) {
        int color = IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
        int alpha = (color >>> 24) & 255;
        if (alpha == 0) {
            alpha = 184;
        }
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static void drawCube(VertexConsumer vertexConsumer, Matrix4f matrix, Matrix3f normal,
                                 int packedLight, int packedOverlay,
                                 float red, float green, float blue, float alpha) {
        float min = -CUBE_RADIUS;
        float max = CUBE_RADIUS;

        // -Y
        addQuad(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha,
                min, min, max, max, min, max, max, min, min, min, min, min, 0.0F, -1.0F, 0.0F);
        // +Y
        addQuad(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha,
                min, max, min, max, max, min, max, max, max, min, max, max, 0.0F, 1.0F, 0.0F);
        // -Z
        addQuad(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha,
                max, min, min, min, min, min, min, max, min, max, max, min, 0.0F, 0.0F, -1.0F);
        // +Z
        addQuad(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha,
                min, min, max, max, min, max, max, max, max, min, max, max, 0.0F, 0.0F, 1.0F);
        // -X
        addQuad(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha,
                min, min, min, min, min, max, min, max, max, min, max, min, -1.0F, 0.0F, 0.0F);
        // +X
        addQuad(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha,
                max, min, max, max, min, min, max, max, min, max, max, max, 1.0F, 0.0F, 0.0F);
    }

    private static void addQuad(VertexConsumer vertexConsumer, Matrix4f matrix, Matrix3f normal,
                                int packedLight, int packedOverlay,
                                float red, float green, float blue, float alpha,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float nx, float ny, float nz) {
        addVertex(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha, x1, y1, z1, nx, ny, nz);
        addVertex(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha, x2, y2, z2, nx, ny, nz);
        addVertex(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha, x3, y3, z3, nx, ny, nz);
        addVertex(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha, x4, y4, z4, nx, ny, nz);
    }

    private static void addVertex(VertexConsumer vertexConsumer, Matrix4f matrix, Matrix3f normal,
                                  int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha,
                                  float x, float y, float z,
                                  float nx, float ny, float nz) {
        vertexConsumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(0.0F, 0.0F)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }
}