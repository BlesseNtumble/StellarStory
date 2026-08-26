package stellarstory.client.events;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import stellarstory.StellarStory;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.IChildBody;
import stellarstory.api.celestial.basic_type.Planet;
import stellarstory.client.gui.SpaceMapScreen;
import stellarstory.client.utils.ClientSpaceData;
import stellarstory.data.CelestialBodyLoader;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = StellarStory.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    private static List<String> text = new ArrayList<>();
    public static float temperatureLevel = 0;

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onOverlayRender(RenderGuiOverlayEvent.Post e) {
        final Minecraft mc = Minecraft.getInstance();
        final Player player = mc.player;

        if(!mc.isPaused() && mc.screen == null && !mc.options.renderDebug) {
            assert player != null;
            text.clear();

            text.add("");
            text.add("Current position:");

            CelestialBody body = CelestialRegisters.getCelestialBodyFromPlayer(player);

            if(body instanceof Planet) {
                text.add(CelestialRegisters.getStarSystemFromStar(((Planet) body).getParentStar().orElseThrow()).getName());
                text.add("   └" + ((Planet) body).getParentStar().orElseThrow().getName());
                text.add("        └" + body.getName());
            }

            if(body instanceof IChildBody) {
                text.add(CelestialRegisters.getStarSystemFromStar(((IChildBody) body).getParentPlanet().getParentStar().orElseThrow()).getName());
                text.add("   └" + ((IChildBody) body).getParentPlanet().getParentStar().orElseThrow().getName());
                text.add("        └" + ((IChildBody) body).getParentPlanet().toString());
                text.add("             └" + body.getName());
            }


            text.add("");
            //text.add("Day Length: " + body.getProperties().getOrCreatePhysicalData().getDayLength());
            text.add("Physical Data:");
            text.add("└ Gravity: " + body.getBodyDataHandler().physicalData().gravity());
            text.add("└ Day Lenght: " + body.getBodyDataHandler().physicalData().dayLength() + " | Current: " + mc.level.getDayTime());
            text.add("");
            text.add("Atmospheric Data:");
            text.add("└ Temperature Nominal: " + body.getBodyDataHandler().atmosphereData().temperature() + " | Mod: " + body.getBodyDataHandler().atmosphereData().temperatureModificator() + " | Offset: " + body.getBodyDataHandler().atmosphereData().temperatureOffset());
            text.add("└ Temperature: " + ClientSpaceData.getTemperature());


            /*
            e.getPoseStack().pushPose();
            e.getPoseStack().scale(1, 1, 0);
            RenderSystem.disableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, StellarCelestial.sol.getIcon());
            mc.gui.blit(e.getPoseStack(), 15, 85, 0, 0, 16, 16, 16, 16);
            e.getPoseStack().popPose();
*/
            int i = 0;
            for(String s : text){
                e.getGuiGraphics().drawString(mc.font, s, 15, 15 + (15 * i++), 0xFFFFFF);
            }

        }
    }
}
