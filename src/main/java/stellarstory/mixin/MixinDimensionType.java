package stellarstory.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;

import java.util.OptionalLong;

@Mixin(DimensionType.class)
public abstract class MixinDimensionType {

    @Shadow
    @Final
    private OptionalLong fixedTime;

    @Shadow
    @Final
    private ResourceLocation effectsLocation;


    @Shadow
    public abstract String toString();

    private CelestialBody celestialBody;

    @Inject(method = "timeOfDay", at = @At("HEAD"), cancellable = true)
    private void timeOfDay(long value, CallbackInfoReturnable<Float> cir) {

        if (celestialBody == null)
            celestialBody = CelestialRegisters.getCelestialBody(this.effectsLocation.getPath());

        if (celestialBody != null && celestialBody.isReachable()) {
            long dayLenght = celestialBody.getBodyDataHandler().physicalData().dayLength();


            double d0 = Mth.frac((double) this.fixedTime.orElse(value) / dayLenght - 0.25D);
            double d1 = 0.5D - Math.cos(d0 * Math.PI) / 2.0D;
            d1 = ((float) (d0 * 2.0D + d1) / 3.0F);

            cir.setReturnValue((float) d1);
        }
    }
}
