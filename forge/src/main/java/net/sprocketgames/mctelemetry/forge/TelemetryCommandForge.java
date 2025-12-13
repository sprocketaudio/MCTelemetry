package net.sprocketgames.mctelemetry.forge;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sprocketgames.mctelemetry.common.PlayerSnapshot;
import net.sprocketgames.mctelemetry.common.TelemetryPayload;
import com.mojang.logging.LogUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Mod.EventBusSubscriber(modid = MCTelemetryForge.MOD_ID)
public class TelemetryCommandForge {
    private static final Logger SERVER_LOGGER = LogManager.getLogger(MinecraftServer.class);

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(buildCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
        return Commands.literal("telemetry")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("json")
                        .then(Commands.argument("nonce", StringArgumentType.word())
                                .executes(context -> {
                                    String nonce = StringArgumentType.getString(context, "nonce");
                                    String json = buildJson(context.getSource());
                                    CommandSourceStack source = context.getSource();
                                    String payload = "TELEMETRY " + nonce + " " + json;

                                    MCTelemetryForge.LOGGER.info(payload);
                                    LogUtils.getLogger().info(payload);
                                    SERVER_LOGGER.info(payload);
                                    System.out.println(payload);
                                    source.getServer().sendSystemMessage(Component.literal(payload));
                                    source.sendSuccess(() -> Component.literal(payload), true);
                                    return 1;
                                })));
    }

    private static String buildJson(CommandSourceStack source) {
        List<PlayerSnapshot> players = source.getServer().getPlayerList().getPlayers()
                .stream()
                .map(TelemetryCommandForge::toSnapshot)
                .toList();

        return TelemetryPayload.build(currentMinecraftVersion(), MCTelemetryForge.LOADER, players);
    }

    private static PlayerSnapshot toSnapshot(ServerPlayer player) {
        String name = player.getGameProfile().getName();
        String uuid = player.getGameProfile().getId().toString().replace("-", "");
        return new PlayerSnapshot(name, uuid);
    }

    private static String currentMinecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }
}
