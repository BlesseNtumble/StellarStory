package stellarstory.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import stellarstory.common.blocks.blockentity.FluidStorageBlockEntity;

/**
 * Отрисовка содержимого жидкостного хранилища: колонна жидкости внутри
 * стеклянного куба, высота пропорциональна заполнению бака.
 */
public class FluidStorageRenderer implements BlockEntityRenderer<FluidStorageBlockEntity> {

    /** Отступ от стенок стеклянного куба: 2/16. */
    private static final float MARGIN = 0.125F;

    /** Высота полного бака: 12/16. */
    private static final float FLUID_HEIGHT = 0.75F;

    public FluidStorageRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull FluidStorageBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (blockEntity.getTankCount() <= 0) {
            return;
        }
        FluidStack fluid = blockEntity.getFluidInTank(0);
        if (fluid.isEmpty()) {
            return;
        }
        float fraction = (float) blockEntity.getFluidAmount(0) / blockEntity.getTankCapacity(0);
        if (fraction <= 0.0F) {
            return;
        }

        int color = FluidRenderHelper.fluidColor(fluid);
        float alpha = ((color >>> 24) & 255) / 255.0F;
        float red = ((color >> 16) & 255) / 255.0F;
        float green = ((color >> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        TextureAtlasSprite sprite = FluidRenderHelper.fluidSprite(fluid);

        float min = MARGIN;
        float max = 1.0F - MARGIN;
        // Верхняя грань жидкости: при полном баке упирается в крышку.
        float topY = fraction >= 0.99F ? max : min + FLUID_HEIGHT * fraction;

        poseStack.pushPose();

        // translucent() куллит обратные грани — отключаем на время отрисовки жидкости.
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        RenderSystem.disableCull();
        FluidRenderHelper.drawBox(vertexConsumer, matrix, normal, packedLight, packedOverlay,
                red, green, blue, alpha, sprite,
                min, min, min, max, topY, max);
        RenderSystem.enableCull();

        poseStack.popPose();
    }
}
