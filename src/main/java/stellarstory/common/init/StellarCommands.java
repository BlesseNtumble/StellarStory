package stellarstory.common.init;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.celestial.basic_type.Station;
import stellarstory.common.transfer.network.PipeNetworkManager;

public class StellarCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Диагностика энергосети: единственный способ не оптимизировать вслепую.
        dispatcher.register(Commands.literal("stellarstory")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("pipe")
                        .then(Commands.literal("stats")
                                .executes(ctx -> {
                                    PipeNetworkManager manager = PipeNetworkManager.find(ctx.getSource().getLevel());
                                    if (manager == null) {
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("Pipe graph is not present in this level"), false);
                                        return 0;
                                    }
                                    String report = String.format(
                                            "networks=%d (active=%d, idle=%d) segments=%d endpoints=%d dirty=%d | tick last=%.1fus avg=%.1fus",
                                            manager.networkCount(),
                                            manager.activeNetworkCount(),
                                            manager.networkCount() - manager.activeNetworkCount(),
                                            manager.segmentCount(),
                                            manager.endpointCount(),
                                            manager.dirtyCount(),
                                            manager.lastTickMicros(),
                                            manager.averageTickMicros());
                                    ctx.getSource().sendSuccess(() -> Component.literal(report), false);
                                    return Command.SINGLE_SUCCESS;
                                }))));

        dispatcher.register(Commands.literal("go_station")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("station", StringArgumentType.word())
                        .executes(ctx -> {
                            CelestialBody body = CelestialRegisters.getCelestialBody(
                                    StringArgumentType.getString(ctx, "station"));
                            if (!(body instanceof Station station)) return 0;
                            return CelestialRegisters.teleportToStation(ctx.getSource().getPlayerOrException(), station)
                                    ? Command.SINGLE_SUCCESS : 0;
                        })));

        // Отладка/тест: открыть станцию игроку
        dispatcher.register(Commands.literal("unlock_station")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("station", StringArgumentType.word())
                        .executes(ctx -> {
                            CelestialBody body = CelestialRegisters.getCelestialBody(
                                    StringArgumentType.getString(ctx, "station"));
                            if (!(body instanceof Station station)) return 0;
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            CelestialRegisters.unlock(player, station);
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("Unlocked station: " + station.getName()), true);
                            return Command.SINGLE_SUCCESS;
                        })));
    }
}
