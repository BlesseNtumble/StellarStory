package stellarstory.network.packet.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.basic_type.Station;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncStationUnlockPacket {
    private final UUID playerUUID;
    private final String stationId;
    private final boolean unlocked;

    public SyncStationUnlockPacket(UUID playerUUID, String stationId, boolean unlocked) {
        this.playerUUID = playerUUID;
        this.stationId = stationId;
        this.unlocked = unlocked;
    }

    // Сериализация в байты
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeUtf(stationId);
        buf.writeBoolean(unlocked);
    }

    // Десериализация из байтов
    public static SyncStationUnlockPacket decode(FriendlyByteBuf buf) {
        return new SyncStationUnlockPacket(
                buf.readUUID(),
                buf.readUtf(),
                buf.readBoolean()
        );
    }

    // Обработка на клиенте
    public void handler(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            // Находим объект станции на клиенте
            CelestialBody body = CelestialRegisters.getCelestialBody(stationId);

            if (body != null && body instanceof Station station) {
                // Обновляем состояние разблокировки
                if (unlocked) {
                    station.unlockForPlayer(playerUUID);
                } else {
                    station.lockForPlayer(playerUUID);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}