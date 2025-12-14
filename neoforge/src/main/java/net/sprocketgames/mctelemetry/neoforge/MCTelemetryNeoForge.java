package net.sprocketgames.mctelemetry.neoforge;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(MCTelemetryNeoForge.MOD_ID)
public class MCTelemetryNeoForge {
    public static final String MOD_ID = "mctelemetry";
    public static final String LOADER = "neoforge";

    public static final Logger LOGGER = LogUtils.getLogger();

    public MCTelemetryNeoForge() {
        registerConfig();
        registerEventListeners();
    }

    private static void registerEventListeners() {
        NeoForge.EVENT_BUS.addListener(TelemetryCommandNeoForge::registerCommands);
        NeoForge.EVENT_BUS.addListener(TelemetryServerHooks::onServerStarted);
        NeoForge.EVENT_BUS.addListener(TelemetryServerHooks::onServerStopping);
        NeoForge.EVENT_BUS.addListener(TelemetryServerHooks::onServerTick);
    }

    private static void registerConfig() {
        try {
            ModLoadingContext.get().registerConfig(TelemetryConfigNeoForge.SPEC, ModConfig.Type.COMMON);
        } catch (Exception e) {
            LOGGER.error("Failed to register telemetry config for NeoForge", e);
        }
    }
}
