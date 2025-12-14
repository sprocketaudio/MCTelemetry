package net.sprocketgames.mctelemetry.neoforge;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
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
            var context = ModLoadingContext.get();

            if (tryRegisterConfig(context, context.getClass(), TelemetryConfigNeoForge.SPEC)) {
                return;
            }

            var container = context.getActiveContainer();
            if (container != null && tryRegisterConfig(container, container.getClass(), TelemetryConfigNeoForge.SPEC)) {
                return;
            }

            LOGGER.error("Failed to register telemetry config for NeoForge: no compatible registerConfig overload found");
        } catch (Exception e) {
            LOGGER.error("Failed to register telemetry config for NeoForge", e);
        }
    }

    private static boolean tryRegisterConfig(Object target, Class<?> targetClass, ModConfigSpec spec) throws Exception {
        Object[][] attempts = new Object[][] {
                {new Class<?>[] {ModConfig.Type.class, ModConfigSpec.class, String.class}, new Object[] {ModConfig.Type.COMMON, spec, MOD_ID + "-common.toml"}},
                {new Class<?>[] {ModConfig.Type.class, ModConfigSpec.class}, new Object[] {ModConfig.Type.COMMON, spec}},
                {new Class<?>[] {ModConfigSpec.class, ModConfig.Type.class}, new Object[] {spec, ModConfig.Type.COMMON}},
                {new Class<?>[] {ModConfigSpec.class, String.class}, new Object[] {spec, MOD_ID + "-common.toml"}},
                {new Class<?>[] {ModConfigSpec.class}, new Object[] {spec}}
        };

        for (Object[] attempt : attempts) {
            Class<?>[] parameterTypes = (Class<?>[]) attempt[0];
            Object[] args = (Object[]) attempt[1];

            try {
                var method = targetClass.getMethod("registerConfig", parameterTypes);
                method.invoke(target, args);
                return true;
            } catch (NoSuchMethodException ignored) {
                // Try next overload
            }
        }

        return false;
    }
}
