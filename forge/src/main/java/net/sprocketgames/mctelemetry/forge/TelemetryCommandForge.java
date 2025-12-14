package net.sprocketgames.mctelemetry.forge;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sprocketgames.mctelemetry.common.PlayerSnapshot;
import net.sprocketgames.mctelemetry.common.TelemetryPayload;
import net.sprocketgames.mctelemetry.common.TelemetrySnapshot;
import net.sprocketgames.mctelemetry.common.server.TelemetryCollector;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = MCTelemetryForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TelemetryCommandForge {
    private static final Logger LOGGER = LogUtils.getLogger();

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
        List<PlayerSnapshot> emptyPlayers = Collections.emptyList();

        try {
            mcVersion = currentMinecraftVersion();
        } catch (Exception e) {
            logDetailed(detailedLogging, "Failed to resolve Minecraft version; using fallback 'unknown'", e);
        }

        try {
            TelemetrySnapshot snapshot = TelemetryCollector.collect(
                    TelemetryServerHooks.asTelemetrySource(source.getServer()),
                    detailedLogging,
                    LOGGER,
                    mcVersion,
                    MCTelemetryForge.LOADER);
            return emitPayload(snapshot, detailedLogging);
        } catch (Exception e) {
            LOGGER.error("Failed while assembling telemetry JSON; returning fallback payload", e);
            return TelemetryPayload.build(mcVersion, MCTelemetryForge.LOADER, emptyPlayers);
        }
    }

    private static String emitPayload(TelemetrySnapshot snapshot, boolean detailedLogging) {
        String payload = TelemetryPayload.build(snapshot);
        logDetailed(detailedLogging, "Telemetry JSON payload built: {}", payload);
        return payload;
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
