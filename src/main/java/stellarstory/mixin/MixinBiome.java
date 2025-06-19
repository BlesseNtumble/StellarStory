package stellarstory.mixin;

import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Biome.class)
public interface MixinBiome  {

    @Accessor("climateSettings")
    Biome.ClimateSettings stellarstory$getClimateSettings();

    @Invoker
    boolean isHumid();
}
