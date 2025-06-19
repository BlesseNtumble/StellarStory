package stellarstory.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import stellarstory.StellarStory;
import stellarstory.network.packets.CGetLevelData;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel PACKET_HANDLER = net.minecraftforge.network.NetworkRegistry.newSimpleChannel(
            new ResourceLocation(StellarStory.MODID, StellarStory.MODID),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int messageId;

    public static void registerPackets() {
        addNetworkMessage(CGetLevelData.class, CGetLevelData::encode, CGetLevelData::decode, CGetLevelData::handle);
    }
    private static <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
        PACKET_HANDLER.registerMessage(messageId, messageType, encoder, decoder, messageConsumer);
        messageId++;
    }

    public static class PacketHandler {

        public static void sendLevelDataPacket(SimpleChannel channel, ServerPlayer player, float temperature) {
            channel.send(PacketDistributor.PLAYER.with(() -> player), new CGetLevelData(temperature));
        }

        public static void sendLevelDataPacketToAll(SimpleChannel channel, float temperature) {
            channel.send(PacketDistributor.ALL.noArg(), new CGetLevelData(temperature));
        }
    }
}
