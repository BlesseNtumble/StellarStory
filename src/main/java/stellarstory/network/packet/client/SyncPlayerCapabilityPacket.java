package stellarstory.network.packet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import stellarstory.common.capability.PlayerCapability;
import net.minecraftforge.network.NetworkEvent.Context;
import stellarstory.common.init.StellarCapabilities;

import java.util.function.Supplier;

public class SyncPlayerCapabilityPacket {
    private static PlayerCapability capability;

    public SyncPlayerCapabilityPacket(FriendlyByteBuf buffer) {
        capability = new PlayerCapability();
        capability.readNBT(buffer.readNbt());
    }

    public SyncPlayerCapabilityPacket(PlayerCapability data) {
        capability = data;
    }

    public static void encode(SyncPlayerCapabilityPacket message, FriendlyByteBuf buffer) {
        buffer.writeNbt((CompoundTag)message.capability.writeNBT());
    }


    public static void handler(SyncPlayerCapabilityPacket message, Supplier<Context> contextSupplier) {
        Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                PlayerCapability variables = Minecraft.getInstance().player.getCapability(StellarCapabilities.PLAYER_CAPABILITY, null).orElse(new PlayerCapability());
            }
        });
        context.setPacketHandled(true);
    }
}
