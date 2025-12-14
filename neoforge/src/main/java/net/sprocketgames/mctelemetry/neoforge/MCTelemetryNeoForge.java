package net.sprocketgames.mctelemetry.neoforge;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(MCTelemetryNeoForge.MOD_ID)
public class MCTelemetryNeoForge {
    public static final String MOD_ID = "mctelemetry";
    public static final String LOADER = "neoforge";

    public static final Logger LOGGER = LogUtils.getLogger();

    public MCTelemetryNeoForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TelemetryConfigNeoForge.SPEC);
    }
}
