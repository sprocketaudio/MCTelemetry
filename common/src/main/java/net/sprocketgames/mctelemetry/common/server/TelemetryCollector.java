package net.sprocketgames.mctelemetry.common.server;

import net.sprocketgames.mctelemetry.common.PlayerSnapshot;
import net.sprocketgames.mctelemetry.common.TelemetrySnapshot;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Shared telemetry collection helpers used by Forge and NeoForge loaders.
 */
public final class TelemetryCollector {
    private TelemetryCollector() {
    }

    public static TelemetrySnapshot collect(TelemetrySource source, boolean detailedLogging, Logger logger, String mcVersion,
                                            String loaderId) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(logger, "logger");

        List<PlayerSnapshot> players = safePlayers(source, detailedLogging, logger);
        TickMetrics metrics = readTickMetrics(source, detailedLogging, logger);

        return TelemetrySnapshot.of(mcVersion, loaderId, players, metrics.mspt(), metrics.tps());
    }

    private static List<PlayerSnapshot> safePlayers(TelemetrySource source, boolean detailedLogging, Logger logger) {
        try {
            List<PlayerSnapshot> players = source.onlinePlayers();
            if (players == null || players.isEmpty()) {
                logDetailed(detailedLogging, logger, "No online players detected; telemetry payload will contain an empty player list");
                return Collections.emptyList();
            }

            logDetailed(detailedLogging, logger, "Snapshotting {} online player(s) for telemetry", players.size());
            return List.copyOf(players);
        } catch (Exception e) {
            logDetailed(detailedLogging, logger, "Failed to fetch online players; proceeding with empty list", e);
            return Collections.emptyList();
        }
    }

    private static TickMetrics readTickMetrics(TelemetrySource source, boolean detailedLogging, Logger logger) {
        try {
            OptionalDouble averageMspt = source.averageTickTimeMs();
            if (averageMspt == null || averageMspt.isEmpty()) {
                logDetailed(detailedLogging, logger, "Average tick time unavailable or invalid; mspt/tps will be null");
                return TickMetrics.empty();
            }

            double mspt = roundToTenth(averageMspt.getAsDouble());
            double tps = mspt > 0 ? roundToTenth(Math.min(20.0, 1000.0 / mspt)) : 20.0;
            return new TickMetrics(mspt, tps);
        } catch (Exception e) {
            logDetailed(detailedLogging, logger, "Failed reading tick timing data", e);
            return TickMetrics.empty();
        }
    }

    private static double roundToTenth(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static void logDetailed(boolean detailedLogging, Logger logger, String message, Object... args) {
        if (detailedLogging) {
            logger.info(message, args);
        }
    }

    public interface TelemetrySource {
        OptionalDouble averageTickTimeMs();

        List<PlayerSnapshot> onlinePlayers();
    }

    private record TickMetrics(Double mspt, Double tps) {
        static TickMetrics empty() {
            return new TickMetrics(null, null);
        }
    }
}
