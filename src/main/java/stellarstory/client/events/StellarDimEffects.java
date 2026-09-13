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
import stellarstory.data.CelestialBodyDataLoader;

@Mod.EventBusSubscriber(modid = StellarStory.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class StellarDimEffects {

    @SubscribeEvent
    public static void registerDimEffects(RegisterDimensionSpecialEffectsEvent e) {
        e.register(ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, "moon"), new MoonSky());
        // station_type.json ссылается на этот id; MoonSky рисует родительскую планету,
        // что для станции, висящей над планетой, и есть нужный вид.
        e.register(ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, "station"), new MoonSky());
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
