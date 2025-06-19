package stellarstory.common.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import stellarstory.StellarStory;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.data.AtmosphereData;
import stellarstory.common.world.savedata.SpaceSavedData;
import stellarstory.network.NetworkHandler;

@Mod.EventBusSubscriber(modid = StellarStory.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WorldTickEvent {


    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if(e.level.isClientSide) return;

        Level level = e.level;
        CelestialBody body = CelestialRegisters.getCelestialBodyFromLevel(level);

        if(body.isReachable()) {
            SpaceSavedData ssd = SpaceSavedData.get(level);
            AtmosphereData atmosphereData = body.getProperties().getOrCreateAtmosphereData();
            float angle = level.getSunAngle(1.0F);
            float value = Mth.cos(angle * Mth.PI * 2.0F);
            float def = atmosphereData.getTemperature();

            value = Math.max(value * def, AtmosphereData.ABSOLUTE_ZERO)
                    * atmosphereData.getTemperatureModificator() + atmosphereData.getTemperatureOffset();

            ssd.setTemperature(value);

            for (Player player : level.players())
                NetworkHandler.PacketHandler.sendLevelDataPacket(NetworkHandler.PACKET_HANDLER, (ServerPlayer) player, value);
        }

    }

}
