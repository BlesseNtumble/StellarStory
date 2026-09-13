package stellarstory.network.packet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import stellarstory.common.capability.PlayerCapability;
import net.minecraftforge.network.NetworkEvent.Context;
import stellarstory.common.init.StellarCapabilities;

import java.util.function.Supplier;

/**
 * Сервер -> клиент: полная копия PlayerCapability (инвентарь скафандра,
 * разблокированные станции). Отправляется при входе, респавне, смене измерения.
 */
public class SyncPlayerCapabilityPacket {
    private final PlayerCapability capability;

    public SyncPlayerCapabilityPacket(FriendlyByteBuf buffer) {
        PlayerCapability data = new PlayerCapability();
        data.readNBT(buffer.readNbt());
        this.capability = data;
    }

    public SyncPlayerCapabilityPacket(PlayerCapability data) {
        this.capability = data;
    }

    public static void encode(SyncPlayerCapabilityPacket message, FriendlyByteBuf buffer) {
        buffer.writeNbt((CompoundTag) message.capability.writeNBT());
    }

    public static void handler(SyncPlayerCapabilityPacket message, Supplier<Context> contextSupplier) {
        Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;

            PlayerCapability local = Minecraft.getInstance().player
                    .getCapability(StellarCapabilities.PLAYER_CAPABILITY, null)
                    .orElse(null);
            if (local != null) {
                local.readNBT(message.capability.writeNBT());
            }
        });
        context.setPacketHandled(true);
    }
}
