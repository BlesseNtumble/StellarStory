package stellarstory.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel extends Level {


    protected MixinClientLevel(WritableLevelData p_220352_, ResourceKey<Level> p_220353_, Holder<DimensionType> p_220354_, Supplier<ProfilerFiller> p_220355_, boolean p_220356_, boolean p_220357_, long p_220358_, int p_220359_) {
        super(p_220352_, p_220353_, p_220354_, p_220355_, p_220356_, p_220357_, p_220358_, p_220359_);
    }

    @Inject(method = "getStarBrightness", at = @At("TAIL"), cancellable = true)
    public void getStarBrightness(float p_104812_, CallbackInfoReturnable<Float> cir) {

        float f = this.getTimeOfDay(p_104812_);
        float f1 = 1.0F - (Mth.cos(f * ((float)Math.PI * 2F)) * 2.0F + 0.25F);
        f1 = Mth.clamp(f1, 0.0F, 1.0F);

        //cir.setReturnValue(f1 * f1 * 0.5F);
        cir.setReturnValue(1.0F);
    }

    @Inject(method = "getSkyDarken", at = @At("HEAD"), cancellable = true)
    public void getSkyDarken(float p_104806_, CallbackInfoReturnable<Float> cir) {
        float f = this.getTimeOfDay(p_104806_);
        float f1 = 1.0F - (Mth.cos(f * 6.2831855F) * 2.0F + 0.2F);
        f1 = Mth.clamp(f1, 0.0F, 1.0F);
        f1 = 1.0F - f1;
        f1 *= 1.0F - this.getRainLevel(p_104806_) * 5.0F / 16.0F;
        f1 *= 1.0F - this.getThunderLevel(p_104806_) * 5.0F / 16.0F;
        cir.setReturnValue(f1 * 0.8F);
    }
}
