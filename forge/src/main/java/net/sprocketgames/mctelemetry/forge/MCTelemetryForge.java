package net.sprocketgames.mctelemetry.forge;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MCTelemetryForge.MOD_ID)
public class MCTelemetryForge {
    public static final String MOD_ID = "mctelemetry";
    public static final String LOADER = "forge";

    public static final Logger LOGGER = LogUtils.getLogger();

    public MCTelemetryForge() {
    }
}
