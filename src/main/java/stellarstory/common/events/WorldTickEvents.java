package stellarstory.common.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import stellarstory.StellarStory;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.data.AtmosphereData;
import stellarstory.common.level.SpaceSavedData;
import stellarstory.common.transfer.network.PipeNetworkManager;
import stellarstory.network.NetworkHandler;
import stellarstory.network.packet.client.SyncSpaceDataPacket;

@Mod.EventBusSubscriber(modid = StellarStory.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WorldTickEvents {


    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.level.isClientSide || e.phase != TickEvent.Phase.END) return;

        Level level = e.level;

        // Граф труб принадлежит измерению и не зависит от небесных тел,
        // поэтому тикается до проверки достижимости тела.
        PipeNetworkManager.tick(level);

        CelestialBody body = CelestialRegisters.getCelestialBodyFromLevel(level);
        if (body == null || !body.isReachable()) return;

        SpaceSavedData ssd = SpaceSavedData.get(level);
        AtmosphereData atm = body.getBodyDataHandler().atmosphereData();

        float time = level.getTimeOfDay(1.0F);
        float solarAngle = (time - 0.0F) * 2.0F * (float) Math.PI;
        float solarIntensity = (float) Math.max(0.0F, Math.cos(solarAngle));

        float mod = Math.max(1.0F, atm.temperatureModificator());
        float dayT = atm.temperature() * mod + atm.temperatureOffset();
        float nightT = atm.temperature() / mod + atm.temperatureOffset();
        float target = nightT + solarIntensity * (dayT - nightT);

        ssd.setTemperature(target);

        ssd.syncTicks++;
        boolean valueChanged = Math.abs(target - ssd.lastSyncedTemp) > 0.05f; // порог 0.05°C
        boolean timeElapsed = ssd.syncTicks >= 100; // ~5 секунд

//        StellarStory.getLogger().debug(String.format("Time: %.3f | Solar: %.3f | Target: %.1f | Curr: %.1f",
//                time, solarIntensity, target, ssd.getTemperature()));

        if (valueChanged || timeElapsed) {
            ssd.lastSyncedTemp = target;
            ssd.syncTicks = 0;

            NetworkHandler.PACKET_HANDLER.send(
                    PacketDistributor.DIMENSION.with(level::dimension),
                    new SyncSpaceDataPacket(target)
            );
        }
    }

    /** Граф труб живёт в измерении: при выгрузке выбрасываем менеджер, иначе течь Level. */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload e) {
        if (e.getLevel() instanceof ServerLevel serverLevel) {
            PipeNetworkManager.onLevelUnload(serverLevel);
        }
    }

    /**
     * Граф труб не сериализуется — после перезахода мира он пуст.
     * Создаём менеджер и просим его отсканировать загруженные машины.
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load e) {
        if (e.getLevel() instanceof ServerLevel serverLevel) {
            PipeNetworkManager.onLevelLoad(serverLevel);
        }
    }

    /**
     * Чанк догрузился позже (игрок уходит в новые чанки): его машины и трубы
     * должны встать в граф, иначе линия на границе чанков не заработает.
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level)) {
            return;
        }
        PipeNetworkManager manager = PipeNetworkManager.find((Level) level);
        if (manager != null) {
            manager.onChunkLoaded(e.getChunk().getPos());
        }
    }
}
