package stellarstory.network.packet.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import stellarstory.client.utils.ClientSpaceData;

import java.util.function.Supplier;

public class SyncSpaceDataPacket {

    private final float temperature;

    public SyncSpaceDataPacket(float temperature) {
        this.temperature = temperature;
    }

    public static void encode(SyncSpaceDataPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.temperature);
    }

    public static SyncSpaceDataPacket decode(FriendlyByteBuf buf) {
        return new SyncSpaceDataPacket(buf.readFloat());
    }

    public static void handler(SyncSpaceDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Сохраняем температуру на клиенте
            ClientSpaceData.setTemperature(packet.temperature);
        });
        ctx.get().setPacketHandled(true);
    }
}
