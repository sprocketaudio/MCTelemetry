package net.sprocketgames.mctelemetry.neoforge;

import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.eventbus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.sprocketgames.mctelemetry.common.TelemetryPayload;
import net.sprocketgames.mctelemetry.common.TelemetrySnapshot;
import net.sprocketgames.mctelemetry.common.server.TelemetryHttpServer;

import java.util.Collections;

@Mod.EventBusSubscriber(modid = MCTelemetryNeoForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.GAME)
public class TelemetryServerHooks {
    private static TelemetryHttpServer httpServer;
    private static int refreshIntervalTicks = TelemetryConfigNeoForge.telemetryRefreshTicks();
    private static int ticksUntilRefresh = refreshIntervalTicks;
    private static String minecraftVersion = SharedConstants.getCurrentVersion().getName();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer()) {
            MCTelemetryNeoForge.LOGGER.info("Skipping telemetry HTTP endpoint; server is not dedicated.");
            return;
        }

        minecraftVersion = SharedConstants.getCurrentVersion().getName();
        refreshIntervalTicks = Math.max(1, TelemetryConfigNeoForge.telemetryRefreshTicks());
        ticksUntilRefresh = refreshIntervalTicks;

        boolean detailedLogging = TelemetryConfigNeoForge.detailedLoggingEnabled();
        String initialPayload = buildPayload(server, detailedLogging);
        int port = TelemetryHttpServer.resolvePort(TelemetryConfigNeoForge.httpPort());
        try {
            httpServer = new TelemetryHttpServer(
                    MCTelemetryNeoForge.LOGGER,
                    initialPayload,
                    port,
                    TelemetryConfigNeoForge.httpBindAddress());
            if (!httpServer.start()) {
                httpServer = null;
                return;
            }
        } catch (IllegalArgumentException e) {
            MCTelemetryNeoForge.LOGGER.error("Failed to configure telemetry HTTP endpoint: {}", e.getMessage());
            httpServer = null;
            return;
        }

        MCTelemetryNeoForge.LOGGER.info(
                "MCTelemetry HTTP endpoint active on {}:{} (interval: {} ticks)",
                httpServer.bindAddress(),
                port,
                refreshIntervalTicks);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (httpServer == null || event.phase != TickEvent.Phase.END) {
            return;
        }

        if (--ticksUntilRefresh > 0) {
            return;
        }

        ticksUntilRefresh = refreshIntervalTicks;

        boolean detailedLogging = TelemetryConfigNeoForge.detailedLoggingEnabled();
        String payload = buildPayload(event.getServer(), detailedLogging);
        httpServer.updateTelemetry(payload);
        logCachedUpdate(detailedLogging, payload);
    }

    private static String buildPayload(MinecraftServer server, boolean detailedLogging) {
        try {
            TelemetrySnapshot snapshot = TelemetryCollector.collect(server, detailedLogging, MCTelemetryNeoForge.LOGGER, minecraftVersion);
            return TelemetryPayload.build(snapshot);
        } catch (Exception e) {
            MCTelemetryNeoForge.LOGGER.warn("Failed to refresh telemetry payload; using fallback", e);
            return TelemetryPayload.build(minecraftVersion, MCTelemetryNeoForge.LOADER, Collections.emptyList());
        }
    }

    private static void logCachedUpdate(boolean detailedLogging, String payload) {
        if (!detailedLogging) {
            return;
        }

        int length = payload == null ? 0 : payload.length();
        MCTelemetryNeoForge.LOGGER.info("Cached telemetry JSON refreshed ({} chars)", length);
    }
}
