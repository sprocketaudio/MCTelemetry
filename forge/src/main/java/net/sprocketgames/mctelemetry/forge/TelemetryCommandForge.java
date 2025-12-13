package net.sprocketgames.mctelemetry.forge;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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

@Mod.EventBusSubscriber(modid = MCTelemetryForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TelemetryCommandForge {
    private static final Logger LOGGER = LogManager.getLogger(TelemetryCommandForge.class);
    private static final Logger ROOT_LOGGER = LogManager.getRootLogger();
    private static final Logger TELEMETRY_LOGGER = LogManager.getLogger("mctelemetry");

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        logEverywhere("RegisterCommandsEvent received: registering /telemetry command");
        event.getDispatcher().register(buildCommand());
        logEverywhere("/telemetry command registration complete");
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
        logEverywhere("Building /telemetry command tree");
        return Commands.literal("telemetry")
                .requires(source -> true)
                .then(Commands.literal("json")
                        .then(Commands.argument("nonce", StringArgumentType.word())
                                .executes(context -> {
                                    String nonce = StringArgumentType.getString(context, "nonce");
                                    CommandSourceStack source = context.getSource();

                                    announceInvocation(nonce);
                                    logAndEcho(source, "Starting telemetry command execution");

                                    try {
                                        logAndEcho(source, "Assembling telemetry payload JSON");
                                        String json = buildJson(source);
                                        logAndEcho(source, "Telemetry JSON payload assembled: " + json);

                                        String payload = "TELEMETRY " + nonce + " " + json;
                                        Component message = Component.literal(payload);

                                        logAndEcho(source, "Broadcasting telemetry payload to players and console");
                                        broadcastPayload(source, message);

                                        logAndEcho(source, "Telemetry command execution completed");
                                        return 1;
                                    } catch (Exception e) {
                                        logErrorEverywhere("Telemetry command failed", e);
                                        try {
                                            source.sendFailure(Component.literal("[MCTelemetry] Telemetry command failed: " + e.getMessage()));
                                        } catch (Exception inner) {
                                            logErrorEverywhere("Failed to send failure message to source", inner);
                                        }
                                        return 0;
                                    }
                                })));
    }

    private static void broadcastPayload(CommandSourceStack source, Component message) {
        source.getServer().getPlayerList().broadcastSystemMessage(message, false);
        source.getServer().sendSystemMessage(message);
        source.sendSystemMessage(message);
        source.sendSuccess(() -> message, true);

        System.out.println("[MCTelemetry] Payload broadcast: " + message.getString());
        System.out.flush();
    }

    private static void announceInvocation(String nonce) {
        String notice = "Telemetry command invoked with nonce: " + nonce;

        LOGGER.info(notice);
        ROOT_LOGGER.info(notice);
        TELEMETRY_LOGGER.info(notice);
        MCTelemetryForge.LOGGER.info(notice);

        System.out.println("[MCTelemetry] " + notice);
        System.out.flush();
    }

    private static void logEverywhere(String payload) {
        MCTelemetryForge.LOGGER.info(payload);
        LogUtils.getLogger().info(payload);
        ROOT_LOGGER.info(payload);
        TELEMETRY_LOGGER.info(payload);

        System.out.println(payload);
        System.out.flush();
    }

    private static void logErrorEverywhere(String message, Throwable error) {
        LOGGER.error(message, error);
        ROOT_LOGGER.error(message, error);
        TELEMETRY_LOGGER.error(message, error);
        MCTelemetryForge.LOGGER.error(message, error);
        LogUtils.getLogger().error(message, error);

        System.err.println(message + ": " + error.getMessage());
        error.printStackTrace(System.err);
        System.err.flush();
    }

    private static void logAndEcho(CommandSourceStack source, String message) {
        logEverywhere(message);

        try {
            source.sendSystemMessage(Component.literal("[MCTelemetry] " + message));
        } catch (Exception e) {
            logEverywhere("Failed to send feedback to command source: " + e.getMessage());
        }
    }

    private static String buildJson(CommandSourceStack source) {
        logEverywhere("Building telemetry JSON payload");

        try {
            logEverywhere("Collecting player snapshots from server: " + source.getServer());

            List<ServerPlayer> onlinePlayers = source.getServer().getPlayerList().getPlayers();
            logEverywhere("Found " + onlinePlayers.size() + " online players to snapshot");

            List<PlayerSnapshot> players = new java.util.ArrayList<>();

            for (ServerPlayer player : onlinePlayers) {
                logEverywhere("Snapshotting player before conversion: " + player.getGameProfile());
                try {
                    PlayerSnapshot snapshot = toSnapshot(player);
                    logEverywhere("Snapshot created for player: " + snapshot.name() + " (" + snapshot.uuid() + ")");
                    players.add(snapshot);
                } catch (Exception e) {
                    logErrorEverywhere("Failed to snapshot player: " + player.getGameProfile(), e);
                    throw e;
                }
            }

            logEverywhere("Collected " + players.size() + " player snapshots for telemetry");

            String mcVersion = currentMinecraftVersion();
            logEverywhere("Using Minecraft version " + mcVersion + " and loader " + MCTelemetryForge.LOADER + " for payload");

            logEverywhere("Invoking TelemetryPayload.build with " + players.size() + " players");
            String payload = TelemetryPayload.build(mcVersion, MCTelemetryForge.LOADER, players);
            logEverywhere("Telemetry JSON payload built: " + payload);

            return payload;
        } catch (Exception e) {
            logErrorEverywhere("Failed while assembling telemetry JSON", e);
            throw e;
        }
    }

    private static PlayerSnapshot toSnapshot(ServerPlayer player) {
        String name = player.getGameProfile().getName();
        String uuid = player.getGameProfile().getId().toString().replace("-", "");
        logEverywhere("Snapshotting player: " + name + " (" + uuid + ")");
        return new PlayerSnapshot(name, uuid);
    }

    private static String currentMinecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }
}
