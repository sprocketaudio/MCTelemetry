package net.sprocketgames.mctelemetry.neoforge;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.eventbus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.sprocketgames.mctelemetry.common.server.TelemetryService;

@Mod.EventBusSubscriber(modid = MCTelemetryNeoForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.GAME)
public class TelemetryServerHooks {
    private static final TelemetryService TELEMETRY_SERVICE = new TelemetryService(MCTelemetryNeoForge.LOADER, MCTelemetryNeoForge.LOGGER);

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
}
