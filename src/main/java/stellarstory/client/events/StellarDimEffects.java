package stellarstory.client.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import stellarstory.StellarStory;
import stellarstory.client.renderer.sky.CelestialSky;
import stellarstory.client.renderer.sky.MoonSky;

@Mod.EventBusSubscriber(modid = StellarStory.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class StellarDimEffects {

    @SubscribeEvent
    public static void registerDimEffects(RegisterDimensionSpecialEffectsEvent e) {
        e.register(ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, "moon"), new MoonSky());
    }
}
