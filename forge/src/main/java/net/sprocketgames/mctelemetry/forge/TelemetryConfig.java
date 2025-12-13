package net.sprocketgames.mctelemetry.forge;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Forge configuration options for the telemetry mod.
 */
public class TelemetryConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final int DEFAULT_HTTP_PORT = 8765;
    private static final String DEFAULT_HTTP_BIND_ADDRESS = "127.0.0.1";
    private static final int DEFAULT_REFRESH_TICKS = 200;

    public static final ForgeConfigSpec.BooleanValue DETAILED_LOGGING = BUILDER
            .comment("Enable detailed telemetry command logging for debugging. When false, only the payload is logged.")
            .define("detailedLogging", false);

    public static final ForgeConfigSpec.IntValue HTTP_PORT = BUILDER
            .comment(
                    "Port for the local telemetry HTTP server (binds to 127.0.0.1 by default).",
                    "Can also be overridden via system property MCTELEMETRY_PORT.")
            .defineInRange("httpPort", DEFAULT_HTTP_PORT, 1, 65535);

    public static final ForgeConfigSpec.ConfigValue<String> HTTP_BIND_ADDRESS = BUILDER
            .comment("Bind address for the local telemetry HTTP server. Defaults to 127.0.0.1 (loopback-only).")
            .define("httpBindAddress", DEFAULT_HTTP_BIND_ADDRESS);

    public static final ForgeConfigSpec.IntValue TELEMETRY_REFRESH_TICKS = BUILDER
            .comment("Number of server ticks between telemetry JSON refreshes that back the HTTP endpoint.")
            .defineInRange("telemetryRefreshTicks", DEFAULT_REFRESH_TICKS, 1, 12000);

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

    public static int httpPort() {
        try {
            return HTTP_PORT.get();
        } catch (IllegalStateException e) {
            MCTelemetryForge.LOGGER.debug("HTTP port config not yet loaded; defaulting to {}", DEFAULT_HTTP_PORT);
            return DEFAULT_HTTP_PORT;
        }
    }

    public static String httpBindAddress() {
        try {
            return HTTP_BIND_ADDRESS.get();
        } catch (IllegalStateException e) {
            MCTelemetryForge.LOGGER.debug("HTTP bind address config not yet loaded; defaulting to {}", DEFAULT_HTTP_BIND_ADDRESS);
            return DEFAULT_HTTP_BIND_ADDRESS;
        }
    }

    public static int telemetryRefreshTicks() {
        try {
            return TELEMETRY_REFRESH_TICKS.get();
        } catch (IllegalStateException e) {
            MCTelemetryForge.LOGGER.debug("Refresh interval config not yet loaded; defaulting to {} ticks", DEFAULT_REFRESH_TICKS);
            return DEFAULT_REFRESH_TICKS;
        }
    }
}
