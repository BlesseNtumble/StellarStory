package stellarstory.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import stellarstory.client.events.ClientEvents;

import java.util.function.Supplier;

public class CGetLevelData {
    private final float levelTemperature;

    public CGetLevelData(float temp) {
        this.levelTemperature = temp;
    }

    public static void encode(CGetLevelData packet, FriendlyByteBuf bytebuf) {
        bytebuf.writeFloat(packet.levelTemperature);
    }

    public static CGetLevelData decode(FriendlyByteBuf bytebuf) {
        return new CGetLevelData(bytebuf.readFloat());
    }

    public static void handle(CGetLevelData packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Обновляем клиентское состояние
            ClientEvents.temperatureLevel = packet.levelTemperature;
        });
        context.setPacketHandled(true);
    }

    public float getLevelTemperature() {
        return levelTemperature;
    }
}
