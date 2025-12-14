package net.sprocketgames.mctelemetry.neoforge;

import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.eventbus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.sprocketgames.mctelemetry.common.PlayerSnapshot;
import net.sprocketgames.mctelemetry.common.server.TelemetryCollector;
import net.sprocketgames.mctelemetry.common.server.TelemetryService;

import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = MCTelemetryNeoForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.GAME)
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
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            TELEMETRY_SERVICE.tick(event.getServer(), TelemetryConfigNeoForge.detailedLoggingEnabled());
        }
    }

    private static TelemetryCollector.TelemetrySource asTelemetrySource(MinecraftServer server) {
        return new TelemetryCollector.TelemetrySource() {
            @Override
            public OptionalDouble averageTickTimeMs() {
                try {
                    double averageMspt = server.getAverageTickTime();
                    return Double.isNaN(averageMspt) || averageMspt <= 0.0 ? OptionalDouble.empty() : OptionalDouble.of(averageMspt);
                } catch (Exception e) {
                    MCTelemetryNeoForge.LOGGER.debug("Failed to read average tick time", e);
                    return OptionalDouble.empty();
                }
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
}
