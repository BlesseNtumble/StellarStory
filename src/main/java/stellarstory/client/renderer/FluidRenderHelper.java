package stellarstory.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Общая отрисовка жидкости в мире: тинт + текстура из атласа + коробка из квадов.
 * Используется рендерерами труб и жидкостного хранилища.
 */
public final class FluidRenderHelper {

    private FluidRenderHelper() {
    }

    /** ARGB жидкости; если альфа в тинте нулевая — ставим рабочую (184/255). */
    public static int fluidColor(FluidStack fluid) {
        int color = IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
        int alpha = (color >>> 24) & 255;
        if (alpha == 0) {
            alpha = 184;
        }
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static TextureAtlasSprite fluidSprite(FluidStack fluid) {
        var still = IClientFluidTypeExtensions.of(fluid.getFluid()).getStillTexture(fluid);
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(still);
    }

    /** Коробка жидкости в локальных координатах (0..1 по блоку уже применён вызывающим). */
    public static void drawBox(VertexConsumer vertexConsumer, Matrix4f matrix, Matrix3f normal,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha,
                               TextureAtlasSprite sprite,
                               float minX, float minY, float minZ,
                               float maxX, float maxY, float maxZ) {
        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float u1 = sprite.getU1();
        float v1 = sprite.getV1();

        // -Y
        addQuad(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha, u0, v0, u1, v1,
                minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ, 0.0F, -1.0F, 0.0F);
        // +Y
        addQuad(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha, u0, v0, u1, v1,
                minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0.0F, 1.0F, 0.0F);
        // -Z
        addQuad(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha, u0, v0, u1, v1,
                maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0.0F, 0.0F, -1.0F);
        // +Z
        addQuad(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha, u0, v0, u1, v1,
                minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0.0F, 0.0F, 1.0F);
        // -X
        addQuad(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha, u0, v0, u1, v1,
                minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1.0F, 0.0F, 0.0F);
        // +X
        addQuad(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha, u0, v0, u1, v1,
                maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1.0F, 0.0F, 0.0F);
    }

    private static void addQuad(VertexConsumer vertexConsumer, Matrix4f matrix, Matrix3f normal,
                                int packedLight, int packedOverlay,
                                float red, float green, float blue, float alpha,
                                float u0, float v0, float u1, float v1,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float nx, float ny, float nz) {
        addVertex(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha, u0, v0,
                x1, y1, z1, nx, ny, nz);
        addVertex(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha, u1, v0,
                x2, y2, z2, nx, ny, nz);
        addVertex(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha, u1, v1,
                x3, y3, z3, nx, ny, nz);
        addVertex(vertexConsumer, matrix, normal, packedLight, packedOverlay, red, green, blue, alpha, u0, v1,
                x4, y4, z4, nx, ny, nz);
    }

    private static void addVertex(VertexConsumer vertexConsumer, Matrix4f matrix, Matrix3f normal,
                                  int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha,
                                  float u, float v,
                                  float x, float y, float z,
                                  float nx, float ny, float nz) {
        vertexConsumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }
}
