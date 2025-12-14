package net.sprocketgames.mctelemetry.neoforge;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.config.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(MCTelemetryNeoForge.MOD_ID)
public class MCTelemetryNeoForge {
    public static final String MOD_ID = "mctelemetry";
    public static final String LOADER = "neoforge";

    public static final Logger LOGGER = LogUtils.getLogger();

    public MCTelemetryNeoForge() {
        registerConfigReflectively();
        registerEventListeners();
    }

    private static void registerEventListeners() {
        NeoForge.EVENT_BUS.addListener(TelemetryCommandNeoForge::registerCommands);
        NeoForge.EVENT_BUS.addListener(TelemetryServerHooks::onServerStarted);
        NeoForge.EVENT_BUS.addListener(TelemetryServerHooks::onServerStopping);
        NeoForge.EVENT_BUS.addListener(TelemetryServerHooks::onServerTick);
    }

    private static void registerConfigReflectively() {
        try {
            Class<?> contextClass = Class.forName("net.neoforged.fml.ModLoadingContext");
            Object context = contextClass.getMethod("get").invoke(null);

            try {
                var register = contextClass.getMethod("registerConfig", ModConfig.Type.class, ModConfigSpec.class);
                register.invoke(context, ModConfig.Type.COMMON, TelemetryConfigNeoForge.SPEC);
                return;
            } catch (NoSuchMethodException ignored) {
                // Fall through to container-based registration.
            }

            Object container = contextClass.getMethod("getActiveContainer").invoke(context);
            Class<?> containerClass = Class.forName("net.neoforged.fml.ModContainer");
            try {
                var register = containerClass.getMethod("registerConfig", ModConfig.Type.class, ModConfigSpec.class);
                register.invoke(container, ModConfig.Type.COMMON, TelemetryConfigNeoForge.SPEC);
                return;
            } catch (NoSuchMethodException ignored) {
                var register = containerClass.getMethod("registerConfig", ModConfigSpec.class);
                register.invoke(container, TelemetryConfigNeoForge.SPEC);
                return;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to register telemetry config for NeoForge", e);
        }
    }
}
