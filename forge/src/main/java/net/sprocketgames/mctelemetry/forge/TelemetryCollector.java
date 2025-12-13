package net.sprocketgames.mctelemetry.forge;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.sprocketgames.mctelemetry.common.PlayerSnapshot;
import net.sprocketgames.mctelemetry.common.TelemetrySnapshot;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Collects telemetry data from the running server for the Forge loader.
 */
class TelemetryCollector {
    private TelemetryCollector() {
    }

    static TelemetrySnapshot collect(CommandSourceStack source, boolean detailedLogging, Logger logger, String mcVersion) {
        return collect(source.getServer(), detailedLogging, logger, mcVersion);
    }

    static TelemetrySnapshot collect(MinecraftServer server, boolean detailedLogging, Logger logger, String mcVersion) {
        List<PlayerSnapshot> players = collectPlayerSnapshots(server, detailedLogging, logger);
        TickMetrics metrics = readTickMetrics(server, detailedLogging, logger);

        return TelemetrySnapshot.of(mcVersion, MCTelemetryForge.LOADER, players, metrics.mspt(), metrics.tps());
    }

    private static TickMetrics readTickMetrics(MinecraftServer server, boolean detailedLogging, Logger logger) {
        OptionalDouble averageMspt = averageTickTimeMs(server, detailedLogging, logger);
        if (averageMspt.isEmpty()) {
            return TickMetrics.empty();
        }

        double mspt = roundToTenth(averageMspt.getAsDouble());
        double tps = mspt > 0 ? roundToTenth(Math.min(20.0, 1000.0 / mspt)) : 20.0;
        return new TickMetrics(mspt, tps);
    }

    private static OptionalDouble averageTickTimeMs(MinecraftServer server, boolean detailedLogging, Logger logger) {
        long[] tickTimes = readTickTimes(server, detailedLogging, logger);
        if (tickTimes.length == 0) {
            return OptionalDouble.empty();
        }

        double totalMs = 0.0;
        int samples = 0;
        for (long time : tickTimes) {
            if (time <= 0) {
                continue;
            }

            totalMs += nanosToMillis(time);
            samples++;
        }

        if (samples == 0) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(totalMs / samples);
    }

    private static long[] readTickTimes(MinecraftServer server, boolean detailedLogging, Logger logger) {
        for (String fieldName : new String[] {"tickTimesNanos", "tickTimes"}) {
            try {
                Field field = MinecraftServer.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(server);
                if (value instanceof long[] ticks && ticks.length > 0) {
                    return ticks.clone();
                }
            } catch (NoSuchFieldException ignored) {
                // Try the next candidate field name.
            } catch (Exception e) {
                logDetailed(detailedLogging, logger, "Failed reading tick timing data via '{}'", fieldName, e);
                break;
            }
        }

        logDetailed(detailedLogging, logger, "Tick timing data unavailable; mspt/tps will be null");
        return new long[0];
    }

    private static List<PlayerSnapshot> collectPlayerSnapshots(MinecraftServer server, boolean detailedLogging, Logger logger) {
        List<PlayerSnapshot> players = new ArrayList<>();

        List<ServerPlayer> onlinePlayers = Collections.emptyList();
        try {
            onlinePlayers = server.getPlayerList().getPlayers();
        } catch (Exception e) {
            logDetailed(detailedLogging, logger, "Failed to fetch online players; proceeding with empty list", e);
        }

        if (onlinePlayers.isEmpty()) {
            logDetailed(detailedLogging, logger, "No online players detected; telemetry payload will contain an empty player list");
            return players;
        }

        logDetailed(detailedLogging, logger, "Snapshotting {} online player(s) for telemetry", onlinePlayers.size());
        for (ServerPlayer player : onlinePlayers) {
            try {
                players.add(toSnapshot(player, detailedLogging, logger));
            } catch (Exception e) {
                logDetailed(detailedLogging, logger, "Failed to snapshot player {}", player.getGameProfile(), e);
            }
        }

        return players;
    }

    private static PlayerSnapshot toSnapshot(ServerPlayer player, boolean detailedLogging, Logger logger) {
        try {
            String name = player.getGameProfile().getName();
            String uuid = player.getGameProfile().getId().toString().replace("-", "");
            return new PlayerSnapshot(name, uuid);
        } catch (Throwable e) {
            logDetailed(detailedLogging, logger, "Error while converting player to snapshot: {}", player, e);
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

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static double roundToTenth(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static void logDetailed(boolean detailedLogging, Logger logger, String message, Object... args) {
        if (detailedLogging) {
            logger.info(message, args);
        }
    }

    private record TickMetrics(Double mspt, Double tps) {
        static TickMetrics empty() {
            return new TickMetrics(null, null);
        }
    }
}
