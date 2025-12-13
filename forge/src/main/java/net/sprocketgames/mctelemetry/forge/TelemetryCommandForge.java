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
import net.sprocketgames.mctelemetry.forge.TelemetryConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = MCTelemetryForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TelemetryCommandForge {
    private static final Logger LOGGER = LogManager.getLogger(TelemetryCommandForge.class);

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        boolean detailedLogging = TelemetryConfig.detailedLoggingEnabled();
        logDetailed(detailedLogging, "Registering /telemetry command");
        event.getDispatcher().register(buildCommand());
        logDetailed(detailedLogging, "/telemetry command registration complete");
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
        return Commands.literal("telemetry")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("json")
                        .then(Commands.argument("nonce", StringArgumentType.word())
                                .executes(context -> {
                                    String nonce = StringArgumentType.getString(context, "nonce");
                                    CommandSourceStack source = context.getSource();
                                    boolean detailedLogging = TelemetryConfig.detailedLoggingEnabled();

                                    logDetailed(detailedLogging, "/telemetry json invoked with nonce '{}'", nonce);

                                    try {
                                        String json = buildJson(source, detailedLogging);
                                        Component message = Component.literal(formatPayload(nonce, json));

                                        broadcastPayload(source, message, detailedLogging);
                                        logDetailed(detailedLogging, "Telemetry payload broadcast for nonce '{}'", nonce);
                                        return 1;
                                    } catch (Exception e) {
                                        LOGGER.error("Telemetry command failed for nonce '{}'", nonce, e);
                                        return 0;
                                    }
                                })));
    }

    private static void broadcastPayload(CommandSourceStack source, Component message, boolean detailedLogging) {
        source.getServer().sendSystemMessage(message);
        logDetailed(detailedLogging, "Payload broadcast to console: {}", message.getString());
    }

    private static String buildJson(CommandSourceStack source, boolean detailedLogging) {
        String mcVersion = "unknown";
        List<PlayerSnapshot> players = collectPlayerSnapshots(source, detailedLogging);

        try {
            mcVersion = currentMinecraftVersion();
        } catch (Exception e) {
            logDetailed(detailedLogging, "Failed to resolve Minecraft version; using fallback 'unknown'", e);
        }

        try {
            return emitPayload(mcVersion, players, detailedLogging);
        } catch (Exception e) {
            LOGGER.error("Failed while assembling telemetry JSON; returning fallback payload", e);
            return TelemetryPayload.build(mcVersion, MCTelemetryForge.LOADER, Collections.emptyList());
        }
    }

    private static String emitPayload(String mcVersion, List<PlayerSnapshot> players, boolean detailedLogging) {
        String payload = TelemetryPayload.build(mcVersion, MCTelemetryForge.LOADER, players);
        logDetailed(detailedLogging, "Telemetry JSON payload built: {}", payload);
        return payload;
    }

    private static List<PlayerSnapshot> collectPlayerSnapshots(CommandSourceStack source, boolean detailedLogging) {
        List<PlayerSnapshot> players = new ArrayList<>();

        List<ServerPlayer> onlinePlayers = Collections.emptyList();
        try {
            onlinePlayers = source.getServer().getPlayerList().getPlayers();
        } catch (Exception e) {
            logDetailed(detailedLogging, "Failed to fetch online players; proceeding with empty list", e);
        }

        if (onlinePlayers.isEmpty()) {
            logDetailed(detailedLogging, "No online players detected; telemetry payload will contain an empty player list");
            return players;
        }

        logDetailed(detailedLogging, "Snapshotting {} online player(s) for telemetry", onlinePlayers.size());
        for (ServerPlayer player : onlinePlayers) {
            try {
                players.add(toSnapshot(player, detailedLogging));
            } catch (Exception e) {
                logDetailed(detailedLogging, "Failed to snapshot player {}", player.getGameProfile(), e);
            }
        }

        return players;
    }

    private static PlayerSnapshot toSnapshot(ServerPlayer player, boolean detailedLogging) {
        try {
            String name = player.getGameProfile().getName();
            String uuid = player.getGameProfile().getId().toString().replace("-", "");
            return new PlayerSnapshot(name, uuid);
        } catch (Throwable e) {
            logDetailed(detailedLogging, "Error while converting player to snapshot: {}", player, e);
            String fallbackName;
            String fallbackUuid;
            try {
                fallbackName = player.getGameProfile().getName();
            } catch (Exception ignored) {
                fallbackName = "unknown";
            }

            try {
                fallbackUuid = player.getGameProfile().getId() == null ? "" : player.getGameProfile().getId().toString().replace("-", "");
            } catch (Exception ignored) {
                fallbackUuid = "";
            }

            return new PlayerSnapshot(fallbackName, fallbackUuid);
        }
    }

    private static String currentMinecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    private static String formatPayload(String nonce, String json) {
        return "TELEMETRY " + nonce + " " + json;
    }

    private static void logDetailed(boolean detailedLogging, String message, Object... args) {
        if (detailedLogging) {
            LOGGER.info(message, args);
        }
    }
}
