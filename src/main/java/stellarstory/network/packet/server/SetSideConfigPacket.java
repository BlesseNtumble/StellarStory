package stellarstory.network.packet.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;
import stellarstory.api.transfer.ISideConfigurable;
import stellarstory.api.transfer.SideMode;
import stellarstory.api.transfer.TransferKind;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

/**
 * Клиент просит изменить режим передачи на грани механизма.
 *
 * Позиция блока обязательна: без неё сервер не мог бы понять, какой именно
 * BlockEntity править. После применения сервер рассылает update-пакет блока,
 * поэтому конфиг доходит до всех клиентов, а не только до автора.
 */
public class SetSideConfigPacket {

    private final BlockPos pos;
    @Nullable
    private final Direction side;
    private final TransferKind kind;
    private final SideMode mode;

    public SetSideConfigPacket(BlockPos pos, @Nullable Direction side, TransferKind kind, SideMode mode) {
        this.pos = pos.immutable();
        this.side = side;
        this.kind = kind;
        this.mode = mode;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(side == null ? 6 : side.ordinal());
        buf.writeByte(kind.ordinal());
        buf.writeByte(mode.ordinal());
    }

    public static SetSideConfigPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int face = buf.readByte();
        int kind = buf.readByte();
        int mode = buf.readByte();
        Direction[] dirs = Direction.values();
        Direction side = (face >= 0 && face < dirs.length) ? dirs[face] : null;
        TransferKind[] kinds = TransferKind.values();
        SideMode[] modes = SideMode.values();
        TransferKind tk = (kind >= 0 && kind < kinds.length) ? kinds[kind] : TransferKind.ENERGY;
        SideMode sm = (mode >= 0 && mode < modes.length) ? modes[mode] : SideMode.DISABLED;
        return new SetSideConfigPacket(pos, side, tk, sm);
    }

    public void handler(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || side == null) {
                return;
            }
            // Чант должен быть загружен, игрок — стоять рядом
            if (!player.level().isLoaded(pos) || !player.blockPosition().closerThan(pos, 8.0D)) {
                return;
            }
            BlockEntity be = player.level().getBlockEntity(pos);
            if (be instanceof ISideConfigurable configurable) {
                configurable.setSideMode(kind, side, mode);
            }
        });
        ctx.setPacketHandled(true);
    }
}
