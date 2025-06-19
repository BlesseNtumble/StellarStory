package stellarstory.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Inject(method = "allowsMultiplayer", at = @At("RETURN"), cancellable = true)
    public void allowsMultiplayer(CallbackInfoReturnable<Boolean> cir) {
        System.out.println("MIXIN: Injected in Minecraft.class");
        cir.setReturnValue(true);
    }
}
