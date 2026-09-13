package stellarstory.common.events;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import stellarstory.StellarStory;
import stellarstory.data.CelestialBodyDataLoader;

/**
 * Регистрирует загрузчик небесных тел на сервере (старт сервера + /reload).
 * Клиентская регистрация — в client.events.StellarDimEffects.
 */
@Mod.EventBusSubscriber(modid = StellarStory.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CelestialDataEvents {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(CelestialBodyDataLoader.INSTANCE);
    }
}
