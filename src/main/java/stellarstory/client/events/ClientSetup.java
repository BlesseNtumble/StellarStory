package stellarstory.client.events;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import stellarstory.StellarStory;
import stellarstory.client.screen.MachineScreen;
import stellarstory.common.init.StellarMenuTypes;

/**
 * Клиентская инициализация мода.
 *
 * В 1.20.1 события RegisterMenuScreensEvent нет, поэтому экран вешается на
 * FMLClientSetupEvent. Слушатель висит на MOD-шине: на MinecraftForge.EVENT_BUS
 * FML-события не доставляются (см. StellarStory#StellarStory).
 */
@Mod.EventBusSubscriber(modid = StellarStory.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(StellarMenuTypes.MACHINE_MENU.get(), MachineScreen::new));
    }
}
