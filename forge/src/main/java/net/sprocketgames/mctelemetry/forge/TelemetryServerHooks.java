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
    private static final int TELEMETRY_REFRESH_TICKS = 200;

    private static TelemetryHttpServer httpServer;
    private static int ticksUntilRefresh = TELEMETRY_REFRESH_TICKS;
    private static String minecraftVersion = SharedConstants.getCurrentVersion().getName();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer()) {
            MCTelemetryForge.LOGGER.info("Skipping telemetry HTTP endpoint; server is not dedicated.");
            return;
        }

        minecraftVersion = SharedConstants.getCurrentVersion().getName();
        String initialPayload = buildPayload(server);
        httpServer = new TelemetryHttpServer(MCTelemetryForge.LOGGER, initialPayload);
        if (!httpServer.start()) {
            httpServer = null;
        }
        ticksUntilRefresh = TELEMETRY_REFRESH_TICKS;
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

        ticksUntilRefresh = TELEMETRY_REFRESH_TICKS;
        httpServer.updateTelemetry(buildPayload(event.getServer()));
    }

    private static String buildPayload(MinecraftServer server) {
        boolean detailedLogging = TelemetryConfig.detailedLoggingEnabled();
        try {
            TelemetrySnapshot snapshot = TelemetryCollector.collect(server, detailedLogging, MCTelemetryForge.LOGGER, minecraftVersion);
            return TelemetryPayload.build(snapshot);
        } catch (Exception e) {
            MCTelemetryForge.LOGGER.warn("Failed to refresh telemetry payload; using fallback", e);
            return TelemetryPayload.build(minecraftVersion, MCTelemetryForge.LOADER, Collections.emptyList());
        }
    }
}
