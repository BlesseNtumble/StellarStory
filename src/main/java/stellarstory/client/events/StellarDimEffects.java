package stellarstory.client.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import stellarstory.StellarStory;
import stellarstory.client.renderer.sky.CelestialSky;
import stellarstory.client.renderer.sky.MoonSky;
import stellarstory.client.renderer.sky.StationSky;
import stellarstory.data.CelestialBodyDataLoader;

@Mod.EventBusSubscriber(modid = StellarStory.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class StellarDimEffects {

    @SubscribeEvent
    public static void registerDimEffects(RegisterDimensionSpecialEffectsEvent e) {
        e.register(ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, "moon"), new MoonSky());
        e.register(ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, "mars_station"), new StationSky());
    }

    /**
     * Парсим celestialbodies на клиенте: в мультиплеере сервер не передаёт
     * тела по сети, обе стороны читают одни и те же json из мода.
     */
    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener(CelestialBodyDataLoader.INSTANCE);
    }
}
