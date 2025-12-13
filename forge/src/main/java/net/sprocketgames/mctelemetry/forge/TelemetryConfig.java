package net.sprocketgames.mctelemetry.forge;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Forge configuration options for the telemetry mod.
 */
public class TelemetryConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DETAILED_LOGGING = BUILDER
            .comment("Enable detailed telemetry command logging for debugging. When false, only the payload is logged.")
            .define("detailedLogging", false);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private TelemetryConfig() {
    }

    public static boolean detailedLoggingEnabled() {
        try {
            return DETAILED_LOGGING.get();
        } catch (IllegalStateException e) {
            MCTelemetryForge.LOGGER.debug("Detailed logging config not yet loaded; defaulting to false");
            return false;
        }
    }
}
