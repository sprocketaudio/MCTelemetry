package net.sprocketgames.mctelemetry.common.server;

import net.sprocketgames.mctelemetry.common.TelemetryPayload;
import net.sprocketgames.mctelemetry.common.TelemetrySnapshot;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;

/**
 * Loader-agnostic helper that owns the lifecycle of the telemetry HTTP server and cached payload.
 */
public class TelemetryService<S> {
    private final String loaderId;
    private final Logger logger;
    private final Function<S, TelemetryCollector.TelemetrySource> telemetrySourceFactory;

    private TelemetryHttpServer httpServer;
    private int refreshIntervalTicks;
    private int ticksUntilRefresh;
    private String minecraftVersion;

    public TelemetryService(String loaderId, Logger logger, Function<S, TelemetryCollector.TelemetrySource> telemetrySourceFactory) {
        this.loaderId = Objects.requireNonNull(loaderId, "loaderId");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.telemetrySourceFactory = Objects.requireNonNull(telemetrySourceFactory, "telemetrySourceFactory");
    }

    public boolean start(S server, String minecraftVersion, boolean detailedLogging, int configuredRefreshTicks, int configuredPort, String configuredBindAddress) {
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
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

    public void tick(S server, boolean detailedLogging) {
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

    private String buildPayload(S server, boolean detailedLogging) {
        try {
            TelemetrySnapshot snapshot = TelemetryCollector.collect(telemetrySourceFactory.apply(server), detailedLogging, logger, minecraftVersion, loaderId);
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
