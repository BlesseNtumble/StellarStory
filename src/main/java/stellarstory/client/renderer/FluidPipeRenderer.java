package stellarstory.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import stellarstory.common.blocks.pipe.FluidPipeBlockEntity;

/**
 * Отрисовка жидкости внутри жидкостной трубы.
 *
 * Сама труба остаётся моделью multipart, а BlockEntity рисует жидкость:
 * кубик в ядре сегмента плюс отрезки в рукавах по направлениям соединений.
 * Геометрия умещается во внутренность оболочки (5..11 пикселей из 16).
 */
public class FluidPipeRenderer implements BlockEntityRenderer<FluidPipeBlockEntity> {

    /** Половина внутреннего сечения трубы: 3/16. */
    private static final float FLUID_HALF = 0.1875F;

    private static final Direction[] DIRS = Direction.values();

    public FluidPipeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull FluidPipeBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        FluidStack fluid = blockEntity.getRenderedFluid();
        if (fluid.isEmpty()) {
            return;
        }

        int color = FluidRenderHelper.fluidColor(fluid);
        float alpha = ((color >>> 24) & 255) / 255.0F;
        float red = ((color >> 16) & 255) / 255.0F;
        float green = ((color >> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        TextureAtlasSprite sprite = FluidRenderHelper.fluidSprite(fluid);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);

        // translucent() куллит обратные грани — отключаем на время отрисовки жидкости.
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        RenderSystem.disableCull();

        // Ядро сегмента.
        FluidRenderHelper.drawBox(vertexConsumer, matrix, normal, packedLight, packedOverlay,
                red, green, blue, alpha, sprite,
                -FLUID_HALF, -FLUID_HALF, -FLUID_HALF, FLUID_HALF, FLUID_HALF, FLUID_HALF);

        // Отрезки жидкости в рукавах по направлениям соединений.
        BlockState state = blockEntity.getBlockState();
        for (Direction dir : DIRS) {
            if (!state.getValue(propertyFor(dir))) {
                continue;
            }
            drawArm(vertexConsumer, matrix, normal, packedLight, packedOverlay,
                    red, green, blue, alpha, sprite, dir);
        }

        RenderSystem.enableCull();

        poseStack.popPose();
    }

    private static BooleanProperty propertyFor(Direction dir) {
        return switch (dir) {
            case DOWN -> BlockStateProperties.DOWN;
            case UP -> BlockStateProperties.UP;
            case NORTH -> BlockStateProperties.NORTH;
            case SOUTH -> BlockStateProperties.SOUTH;
            case WEST -> BlockStateProperties.WEST;
            case EAST -> BlockStateProperties.EAST;
        };
    }

    /** Отрезок жидкости от ядра до грани блока по направлению dir. */
    private static void drawArm(VertexConsumer vertexConsumer, Matrix4f matrix, Matrix3f normal,
                                int packedLight, int packedOverlay,
                                float red, float green, float blue, float alpha,
                                TextureAtlasSprite sprite, Direction dir) {
        float minX = -FLUID_HALF;
        float minY = -FLUID_HALF;
        float minZ = -FLUID_HALF;
        float maxX = FLUID_HALF;
        float maxY = FLUID_HALF;
        float maxZ = FLUID_HALF;
        switch (dir) {
            case DOWN -> minY = -0.5F;
            case UP -> maxY = 0.5F;
            case NORTH -> minZ = -0.5F;
            case SOUTH -> maxZ = 0.5F;
            case WEST -> minX = -0.5F;
            case EAST -> maxX = 0.5F;
        }
        FluidRenderHelper.drawBox(vertexConsumer, matrix, normal, packedLight, packedOverlay,
                red, green, blue, alpha, sprite, minX, minY, minZ, maxX, maxY, maxZ);
    }
}
