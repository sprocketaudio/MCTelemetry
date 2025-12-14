package net.sprocketgames.mctelemetry.forge;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.sprocketgames.mctelemetry.common.server.TelemetryService;

@Mod.EventBusSubscriber(modid = MCTelemetryForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TelemetryServerHooks {
    private static final TelemetryService TELEMETRY_SERVICE = new TelemetryService(MCTelemetryForge.LOADER, MCTelemetryForge.LOGGER);

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer()) {
            MCTelemetryForge.LOGGER.info("Skipping telemetry HTTP endpoint; server is not dedicated.");
            return;
        }

        boolean detailedLogging = TelemetryConfig.detailedLoggingEnabled();
        TELEMETRY_SERVICE.start(
                server,
                detailedLogging,
                TelemetryConfig.telemetryRefreshTicks(),
                TelemetryConfig.httpPort(),
                TelemetryConfig.httpBindAddress());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        TELEMETRY_SERVICE.stop();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            TELEMETRY_SERVICE.tick(event.getServer(), TelemetryConfig.detailedLoggingEnabled());
        }
    }
}
