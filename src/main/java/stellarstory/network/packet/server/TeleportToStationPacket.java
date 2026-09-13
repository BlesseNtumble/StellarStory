package stellarstory.network.packet.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.basic_type.Station;

import java.util.function.Supplier;

/**
 * Клиент просит сервер отправить его на станцию (используется из SpaceMapScreen).
 */
public class TeleportToStationPacket {
    private final String stationName;

    public TeleportToStationPacket(String stationName) {
        this.stationName = stationName;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(stationName);
    }

    public static TeleportToStationPacket decode(FriendlyByteBuf buf) {
        return new TeleportToStationPacket(buf.readUtf());
    }

    public void handler(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            CelestialBody body = CelestialRegisters.getCelestialBody(stationName);
            if (!(body instanceof Station station)) return;

            CelestialRegisters.teleportToStation(player, station);
        });
        ctx.setPacketHandled(true);
    }
}
