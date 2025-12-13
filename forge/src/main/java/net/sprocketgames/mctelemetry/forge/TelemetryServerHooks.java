package net.sprocketgames.mctelemetry.forge;

import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.sprocketgames.mctelemetry.common.TelemetryPayload;
import net.sprocketgames.mctelemetry.common.TelemetrySnapshot;

import java.util.Collections;

@Mod.EventBusSubscriber(modid = MCTelemetryForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TelemetryServerHooks {
    private static TelemetryHttpServer httpServer;
    private static int refreshIntervalTicks = TelemetryConfig.telemetryRefreshTicks();
    private static int ticksUntilRefresh = refreshIntervalTicks;
    private static String minecraftVersion = SharedConstants.getCurrentVersion().getName();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer()) {
            MCTelemetryForge.LOGGER.info("Skipping telemetry HTTP endpoint; server is not dedicated.");
            return;
        }

        minecraftVersion = SharedConstants.getCurrentVersion().getName();
        refreshIntervalTicks = Math.max(1, TelemetryConfig.telemetryRefreshTicks());
        ticksUntilRefresh = refreshIntervalTicks;

        boolean detailedLogging = TelemetryConfig.detailedLoggingEnabled();
        String initialPayload = buildPayload(server, detailedLogging);
        int port = TelemetryHttpServer.resolvePort(TelemetryConfig.httpPort());
        httpServer = new TelemetryHttpServer(
                MCTelemetryForge.LOGGER,
                initialPayload,
                port,
                TelemetryConfig.httpBindAddress());
        if (!httpServer.start()) {
            httpServer = null;
            return;
        }

        MCTelemetryForge.LOGGER.info(
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

        boolean detailedLogging = TelemetryConfig.detailedLoggingEnabled();
        String payload = buildPayload(event.getServer(), detailedLogging);
        httpServer.updateTelemetry(payload);
        logCachedUpdate(detailedLogging, payload);
    }

    private static String buildPayload(MinecraftServer server, boolean detailedLogging) {
        try {
            TelemetrySnapshot snapshot = TelemetryCollector.collect(server, detailedLogging, MCTelemetryForge.LOGGER, minecraftVersion);
            return TelemetryPayload.build(snapshot);
        } catch (Exception e) {
            MCTelemetryForge.LOGGER.warn("Failed to refresh telemetry payload; using fallback", e);
            return TelemetryPayload.build(minecraftVersion, MCTelemetryForge.LOADER, Collections.emptyList());
        }
    }

    private static void logCachedUpdate(boolean detailedLogging, String payload) {
        if (!detailedLogging) {
            return;
        }

        int length = payload == null ? 0 : payload.length();
        MCTelemetryForge.LOGGER.info("Cached telemetry JSON refreshed ({} chars)", length);
    }
}
