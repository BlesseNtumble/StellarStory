package stellarstory.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.simple.SimpleChannel;
import stellarstory.StellarStory;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent.Context;
import stellarstory.network.packet.client.SyncPlayerCapabilityPacket;
import stellarstory.network.packet.client.SyncSpaceDataPacket;
import stellarstory.network.packet.client.SyncStationUnlockPacket;

@Mod.EventBusSubscriber(
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel PACKET_HANDLER = net.minecraftforge.network.NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, StellarStory.MODID),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int messageId;

    public static <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<Context>> messageConsumer) {
        PACKET_HANDLER.registerMessage(messageId, messageType, encoder, decoder, messageConsumer);
        ++messageId;
    }

    @SubscribeEvent
    public static void registerMessages(FMLCommonSetupEvent event) {
        addNetworkMessage(SyncSpaceDataPacket.class, SyncSpaceDataPacket::encode, SyncSpaceDataPacket::decode, SyncSpaceDataPacket::handler);
        addNetworkMessage(SyncPlayerCapabilityPacket.class, SyncPlayerCapabilityPacket::encode, SyncPlayerCapabilityPacket::new, SyncPlayerCapabilityPacket::handler);
        addNetworkMessage(SyncStationUnlockPacket.class, SyncStationUnlockPacket::encode, SyncStationUnlockPacket::decode, SyncStationUnlockPacket::handler);
    }
}
