package stellarstory.network.packet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import stellarstory.common.capability.PlayerCapability;
import stellarstory.common.init.StellarCapabilities;

import java.util.function.Supplier;

/**
 * Сервер -> клиент: точечное обновление разблокировки одной станции.
 * Хранится в PlayerCapability клиента, переживает перезаход.
 */
public class SyncStationUnlockPacket {
    private final String stationName;
    private final boolean unlocked;

    public SyncStationUnlockPacket(String stationName, boolean unlocked) {
        this.stationName = stationName;
        this.unlocked = unlocked;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(stationName);
        buf.writeBoolean(unlocked);
    }

    public static SyncStationUnlockPacket decode(FriendlyByteBuf buf) {
        return new SyncStationUnlockPacket(buf.readUtf(), buf.readBoolean());
    }

    public void handler(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;

            PlayerCapability cap = Minecraft.getInstance().player
                    .getCapability(StellarCapabilities.PLAYER_CAPABILITY, null)
                    .orElse(null);
            if (cap == null) return;

            if (unlocked) {
                cap.unlockStation(stationName);
            } else {
                cap.lockStation(stationName);
            }
        });
        ctx.setPacketHandled(true);
    }
}