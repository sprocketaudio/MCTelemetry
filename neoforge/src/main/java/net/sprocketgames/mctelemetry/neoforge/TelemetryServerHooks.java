package net.sprocketgames.mctelemetry.neoforge;

import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.sprocketgames.mctelemetry.common.PlayerSnapshot;
import net.sprocketgames.mctelemetry.common.server.TelemetryCollector;
import net.sprocketgames.mctelemetry.common.server.TelemetryService;

import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public class TelemetryServerHooks {
    private static final TelemetryService<MinecraftServer> TELEMETRY_SERVICE = new TelemetryService<>(
            MCTelemetryNeoForge.LOADER,
            MCTelemetryNeoForge.LOGGER,
            TelemetryServerHooks::asTelemetrySource);

    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer()) {
            MCTelemetryNeoForge.LOGGER.info("Skipping telemetry HTTP endpoint; server is not dedicated.");
            return;
        }

        boolean detailedLogging = TelemetryConfigNeoForge.detailedLoggingEnabled();
        TELEMETRY_SERVICE.start(
                server,
                SharedConstants.getCurrentVersion().getName(),
                detailedLogging,
                TelemetryConfigNeoForge.telemetryRefreshTicks(),
                TelemetryConfigNeoForge.httpPort(),
                TelemetryConfigNeoForge.httpBindAddress());
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        TELEMETRY_SERVICE.stop();
    }

    public static void onServerTick(Post event) {
        TELEMETRY_SERVICE.tick(event.getServer(), TelemetryConfigNeoForge.detailedLoggingEnabled());
    }

    static TelemetryCollector.TelemetrySource asTelemetrySource(MinecraftServer server) {
        return new TelemetryCollector.TelemetrySource() {
            @Override
            public OptionalDouble averageTickTimeMs() {
                return TelemetryServerHooks.readAverageTickTime(server);
            }

            @Override
            public List<PlayerSnapshot> onlinePlayers() {
                try {
                    return server.getPlayerList().getPlayers().stream()
                            .map(TelemetryServerHooks::snapshotForPlayer)
                            .collect(Collectors.toList());
                } catch (Exception e) {
                    MCTelemetryNeoForge.LOGGER.debug("Failed to gather online players", e);
                    return Collections.emptyList();
                }
            }
        };
    }

    private static PlayerSnapshot snapshotForPlayer(ServerPlayer player) {
        try {
            String name = player.getGameProfile().getName();
            String uuid = player.getGameProfile().getId().toString().replace("-", "");
            return new PlayerSnapshot(name, uuid);
        } catch (Exception e) {
            MCTelemetryNeoForge.LOGGER.debug("Failed to snapshot player {}", player, e);
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

    private static OptionalDouble readAverageTickTime(MinecraftServer server) {
        try {
            var method = server.getClass().getMethod("getAverageTickTime");
            Object result = method.invoke(server);
            if (result instanceof Number number) {
                double averageMspt = number.doubleValue();
                return Double.isNaN(averageMspt) || averageMspt <= 0.0 ? OptionalDouble.empty() : OptionalDouble.of(averageMspt);
            }
        } catch (NoSuchMethodException ignored) {
            // Fall through to nanos-based accessor below.
        } catch (Exception e) {
            MCTelemetryNeoForge.LOGGER.debug("Failed to read average tick time via getAverageTickTime", e);
            return OptionalDouble.empty();
        }

        try {
            var method = server.getClass().getMethod("getAverageTickTimeNanos");
            Object result = method.invoke(server);
            if (result instanceof Number number) {
                double averageMspt = number.doubleValue() / 1_000_000.0d;
                return Double.isNaN(averageMspt) || averageMspt <= 0.0 ? OptionalDouble.empty() : OptionalDouble.of(averageMspt);
            }
        } catch (NoSuchMethodException ignored) {
            // No fallback available; return empty below.
        } catch (Exception e) {
            MCTelemetryNeoForge.LOGGER.debug("Failed to read average tick time via getAverageTickTimeNanos", e);
        }

        return OptionalDouble.empty();
    }
}
