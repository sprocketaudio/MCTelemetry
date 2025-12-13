package net.sprocketgames.mctelemetry;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MCTelemetry.MOD_ID)
public class MCTelemetry
{
    public static final String MOD_ID = "mctelemetry";
    public static final String MINECRAFT_VERSION = "1.20.1";
    public static final String LOADER = "forge";

    public static final Logger LOGGER = LogUtils.getLogger();

    public MCTelemetry()
    {
        // No initialization needed for this minimal telemetry mod
    }
}
