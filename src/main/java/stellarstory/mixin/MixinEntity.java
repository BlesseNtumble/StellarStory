package stellarstory.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import stellarstory.api.utils.event.EntityTickEvent;

@Mixin(Entity.class)
public class MixinEntity {

    @Inject(at = @At(value = "HEAD"), method = "tick")
    private void stellarstory$preTick(CallbackInfo info) {

        Entity entity = (Entity) ((Object) this);
        MinecraftForge.EVENT_BUS.post(new EntityTickEvent.Pre(entity));
    }

    @Inject(at = @At(value = "TAIL"), method = "tick")
    private void stellarstory$postTick(CallbackInfo info) {

        Entity entity = (Entity) ((Object) this);
        MinecraftForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
    }
}
