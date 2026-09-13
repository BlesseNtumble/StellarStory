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

public class StellarCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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
