package net.sprocketgames.mctelemetry.neoforge;

import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.sprocketgames.mctelemetry.common.PlayerSnapshot;
import net.sprocketgames.mctelemetry.common.server.TelemetryCollector;
import net.sprocketgames.mctelemetry.common.server.TelemetryService;

import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = MCTelemetryNeoForge.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class TelemetryServerHooks {
    private static final TelemetryService<MinecraftServer> TELEMETRY_SERVICE = new TelemetryService<>(
            MCTelemetryNeoForge.LOADER,
            MCTelemetryNeoForge.LOGGER,
            TelemetryServerHooks::asTelemetrySource);

    @SubscribeEvent
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

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        TELEMETRY_SERVICE.stop();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (isEndPhase(event)) {
            TELEMETRY_SERVICE.tick(event.getServer(), TelemetryConfigNeoForge.detailedLoggingEnabled());
        }
    }

    private static boolean isEndPhase(ServerTickEvent event) {
        try {
            var method = event.getClass().getMethod("getPhase");
            Object phase = method.invoke(event);
            return phase != null && "END".equals(phase.toString());
        } catch (ReflectiveOperationException ignored) {
            // Fall back to a field-based lookup used by some API variants.
        }

        try {
            var field = event.getClass().getField("phase");
            Object phase = field.get(event);
            return phase != null && "END".equals(phase.toString());
        } catch (ReflectiveOperationException ignored) {
            // If no phase accessor is present, treat the event as the end phase to avoid missing ticks.
        }

        return true;
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
