package stellarstory.client.events;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import stellarstory.StellarStory;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.IChildBody;
import stellarstory.api.celestial.basic_type.Planet;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = StellarStory.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
                text.add(CelestialRegisters.getStarSystemFromStar(((IChildBody) body).getParentPlanet().getParentStar().orElseThrow()).toString());
                text.add("   └" + ((IChildBody) body).getParentPlanet().getParentStar().toString());
                text.add("        └" + ((IChildBody) body).getParentPlanet().toString());
                text.add("             └" + body.getName());
            }


            text.add("");
            //text.add("Day Length: " + body.getProperties().getOrCreatePhysicalData().getDayLength());
            //text.add("Gravity: " + body.getProperties().getOrCreatePhysicalData().getGravity());
            //text.add("Temperature Nominal: " + body.getProperties().getOrCreateAtmosphereData().getTemperature());
            text.add("Temperature: " + temperatureLevel);

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
