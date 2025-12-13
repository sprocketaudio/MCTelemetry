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
        try {
            source.getServer().getPlayerList().broadcastSystemMessage(message, false);
            logEverywhere("Broadcasted payload to all players");
        } catch (Exception e) {
            logErrorEverywhere("Failed to broadcast payload to players", e);
        }

        try {
            source.getServer().sendSystemMessage(message);
            logEverywhere("Sent payload to server system message");
        } catch (Exception e) {
            logErrorEverywhere("Failed to send payload to server", e);
        }

        try {
            source.sendSystemMessage(message);
            logEverywhere("Sent payload to command source system message");
        } catch (Exception e) {
            logErrorEverywhere("Failed to send payload to command source", e);
        }

        try {
            source.sendSuccess(() -> message, true);
            logEverywhere("Sent payload success response to command source");
        } catch (Exception e) {
            logErrorEverywhere("Failed to send success response to command source", e);
        }

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

        String mcVersion = "unknown";
        List<PlayerSnapshot> players = new java.util.ArrayList<>();

        safeLog("buildJson entered; initial player list size=" + players.size());

        logEverywhere("Collecting player snapshots from server: " + source.getServer());

        List<ServerPlayer> onlinePlayers = java.util.Collections.emptyList();
        try {
            onlinePlayers = source.getServer().getPlayerList().getPlayers();
            logEverywhere("Player list fetched; found " + onlinePlayers.size() + " online players to snapshot");
        } catch (Exception e) {
            logErrorEverywhere("Failed to fetch online players", e);
            logEverywhere("Proceeding with empty player list after fetch failure");
        }

        for (int i = 0; i < onlinePlayers.size(); i++) {
            ServerPlayer player = onlinePlayers.get(i);
            safeLog("Snapshot loop entry index=" + i + ", playersSoFar=" + players.size());
            try {
                safeLog("Snapshotting player before conversion (index " + i + "): " + player.getGameProfile());
            } catch (Exception e) {
                logErrorEverywhere("Failed to log player game profile at index " + i, e);
            }

            try {
                PlayerSnapshot snapshot = toSnapshot(player);
                safeLog("Snapshot object created (index " + i + "): name=" + snapshot.name() + ", uuid=" + snapshot.uuid());
                try {
                    players.add(snapshot);
                    System.out.println("[MCTelemetry] Players list size after add at index " + i + " -> " + players.size());
                    System.out.flush();
                    safeLog("Snapshot addition complete for index " + i);
                } catch (Exception addError) {
                    logErrorEverywhere("Failed to add snapshot to list (index " + i + ")", addError);
                }
            } catch (Exception e) {
                logErrorEverywhere("Failed to snapshot player (index " + i + "): " + player.getGameProfile(), e);
                logEverywhere("Continuing after failed snapshot for index " + i);
            }

            try {
                safeLog("Snapshot loop post-step index=" + i + ", playersSoFar=" + players.size());
            } catch (Exception e) {
                logErrorEverywhere("Failed to log post-step state for index " + i, e);
            }
        }

        System.out.println("[MCTelemetry] Snapshot loop finished with " + players.size() + " entries");
        System.out.flush();
        logEverywhere("Player snapshot loop complete; collected " + players.size() + " player snapshots for telemetry");
        for (int i = 0; i < players.size(); i++) {
            PlayerSnapshot snapshot = players.get(i);
            logEverywhere("Snapshot summary (index " + i + "): name=" + snapshot.name() + ", uuid=" + snapshot.uuid());
        }

        try {
            logEverywhere("Resolving current Minecraft version via SharedConstants");
            mcVersion = currentMinecraftVersion();
            logEverywhere("Minecraft version resolved as " + mcVersion);
        } catch (Exception e) {
            logErrorEverywhere("Failed to resolve Minecraft version", e);
            logEverywhere("Proceeding with fallback minecraft version '" + mcVersion + "'");
        }

        String payload = null;
        try {
            logEverywhere("Entering telemetry payload build with " + players.size() + " players and version " + mcVersion);
            payload = emitPayload(mcVersion, players);
            logEverywhere("buildJson completed normally");
        } catch (Exception e) {
            logErrorEverywhere("Failed while assembling telemetry JSON", e);
            payload = "{\"mc\":\"" + mcVersion + "\",\"loader\":\"" + MCTelemetryForge.LOADER + "\",\"players\":[]}";
            logEverywhere("Returning fallback telemetry JSON due to failure: " + payload);
        } finally {
            System.out.println("[MCTelemetry] buildJson exiting; mcVersion=" + mcVersion + ", players=" + players.size() + ", payloadLength=" + (payload == null ? "null" : payload.length()));
            if (payload != null) {
                System.out.println("[MCTelemetry] buildJson payload preview: " + payload);
            }
            System.out.flush();
            System.err.flush();
        }

        return payload;
    }

    private static String emitPayload(String mcVersion, List<PlayerSnapshot> players) {
        logEverywhere("Preparing to invoke TelemetryPayload.build with loader=" + MCTelemetryForge.LOADER + " and " + players.size() + " players");
        System.out.println("[MCTelemetry] About to build telemetry JSON with " + players.size() + " players");
        System.out.flush();
        try {
            String payload = TelemetryPayload.build(mcVersion, MCTelemetryForge.LOADER, players);
            logEverywhere("Telemetry JSON payload built: " + payload);
            System.out.println("[MCTelemetry] Telemetry JSON: " + payload);
            System.out.flush();
            return payload;
        } catch (Throwable e) {
            logErrorEverywhere("TelemetryPayload.build threw an exception", e);
            try {
                e.printStackTrace(System.err);
                System.err.flush();
            } catch (Exception ignored) {
            }
            String fallback = "{\"mc\":\"" + mcVersion + "\",\"loader\":\"" + MCTelemetryForge.LOADER + "\",\"players\":[]}";
            System.out.println("[MCTelemetry] Using fallback telemetry JSON due to build failure: " + fallback);
            System.out.flush();
            return fallback;
        }
    }

    private static PlayerSnapshot toSnapshot(ServerPlayer player) {
        try {
            String name = player.getGameProfile().getName();
            String uuid = player.getGameProfile().getId().toString().replace("-", "");
            logEverywhere("Snapshotting player: " + name + " (" + uuid + ")");
            return new PlayerSnapshot(name, uuid);
        } catch (Throwable e) {
            logErrorEverywhere("Error while converting player to snapshot: " + player, e);
            try {
                e.printStackTrace(System.err);
                System.err.flush();
            } catch (Exception ignored) {
            }
            // Continue with a minimal snapshot so downstream payload building can still complete.
            String fallbackName;
            String fallbackUuid;
            try {
                fallbackName = player.getGameProfile().getName();
            } catch (Exception inner) {
                fallbackName = "unknown";
            }

            try {
                fallbackUuid = player.getGameProfile().getId() == null ? "" : player.getGameProfile().getId().toString().replace("-", "");
            } catch (Exception inner) {
                fallbackUuid = "";
            }

            return new PlayerSnapshot(fallbackName, fallbackUuid);
        }
    }

    private static String currentMinecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    private static void safeLog(String message) {
        try {
            logEverywhere(message);
        } catch (Exception e) {
            try {
                System.err.println("[MCTelemetry] Logging failure: " + message + " -> " + e.getMessage());
                e.printStackTrace(System.err);
                System.err.flush();
            } catch (Exception inner) {
                // Last resort logging guard
            }
        }
    }
}
