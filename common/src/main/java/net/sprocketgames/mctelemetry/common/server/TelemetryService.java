package net.sprocketgames.mctelemetry.common.server;

import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.sprocketgames.mctelemetry.common.TelemetryPayload;
import net.sprocketgames.mctelemetry.common.TelemetrySnapshot;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Objects;

/**
 * Loader-agnostic helper that owns the lifecycle of the telemetry HTTP server and cached payload.
 */
public class TelemetryService {
    private final String loaderId;
    private final Logger logger;

    private TelemetryHttpServer httpServer;
    private int refreshIntervalTicks;
    private int ticksUntilRefresh;
    private String minecraftVersion = SharedConstants.getCurrentVersion().getName();

    public TelemetryService(String loaderId, Logger logger) {
        this.loaderId = Objects.requireNonNull(loaderId, "loaderId");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public boolean start(MinecraftServer server, boolean detailedLogging, int configuredRefreshTicks, int configuredPort, String configuredBindAddress) {
        minecraftVersion = SharedConstants.getCurrentVersion().getName();
        refreshIntervalTicks = Math.max(1, configuredRefreshTicks);
        ticksUntilRefresh = refreshIntervalTicks;

        String initialPayload = buildPayload(server, detailedLogging);
        int port = TelemetryHttpServer.resolvePort(configuredPort);
        try {
            httpServer = new TelemetryHttpServer(logger, initialPayload, port, configuredBindAddress);
            if (!httpServer.start()) {
                httpServer = null;
                return false;
            }
        } catch (IllegalArgumentException e) {
            logger.error("Failed to configure telemetry HTTP endpoint: {}", e.getMessage());
            httpServer = null;
            return false;
        }

        logger.info(
                "MCTelemetry HTTP endpoint active on {}:{} (interval: {} ticks)",
                httpServer.bindAddress(),
                port,
                refreshIntervalTicks);
        return true;
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }
    }

    public void tick(MinecraftServer server, boolean detailedLogging) {
        if (httpServer == null) {
            return;
        }

        if (--ticksUntilRefresh > 0) {
            return;
        }

        ticksUntilRefresh = refreshIntervalTicks;

        String payload = buildPayload(server, detailedLogging);
        httpServer.updateTelemetry(payload);
        logCachedUpdate(detailedLogging, payload);
    }

    private String buildPayload(MinecraftServer server, boolean detailedLogging) {
        try {
            TelemetrySnapshot snapshot = TelemetryCollector.collect(server, detailedLogging, logger, minecraftVersion, loaderId);
            return TelemetryPayload.build(snapshot);
        } catch (Exception e) {
            logger.warn("Failed to refresh telemetry payload; using fallback", e);
            return TelemetryPayload.build(minecraftVersion, loaderId, Collections.emptyList());
        }
    }

    private void logCachedUpdate(boolean detailedLogging, String payload) {
        if (!detailedLogging) {
            return;
        }

        int length = payload == null ? 0 : payload.length();
        logger.info("Cached telemetry JSON refreshed ({} chars)", length);
    }
}
