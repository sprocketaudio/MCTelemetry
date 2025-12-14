package net.sprocketgames.mctelemetry.neoforge;

import net.neoforged.neoforge.common.config.ModConfigSpec;

public class TelemetryConfigNeoForge {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final int DEFAULT_HTTP_PORT = 8765;
    private static final String DEFAULT_HTTP_BIND_ADDRESS = "127.0.0.1";
    private static final int DEFAULT_REFRESH_TICKS = 200;
    private static boolean detailedLoggingFallbackLogged = false;

    public static final ModConfigSpec.BooleanValue DETAILED_LOGGING = BUILDER
            .comment("Enable detailed telemetry command logging for debugging. When false, only the payload is logged.")
            .define("detailedLogging", false);

    public static final ModConfigSpec.IntValue HTTP_PORT = BUILDER
            .comment(
                    "Port for the local telemetry HTTP server (binds to 127.0.0.1 by default).",
                    "Can also be overridden via system property MCTELEMETRY_PORT.")
            .defineInRange("httpPort", DEFAULT_HTTP_PORT, 1, 65535);

    public static final ModConfigSpec.ConfigValue<String> HTTP_BIND_ADDRESS = BUILDER
            .comment(
                    "Bind address for the local telemetry HTTP server. Defaults to 127.0.0.1 (loopback-only).",
                    "Use 0.0.0.0 when running inside Docker/Pterodactyl.",
                    "Can also be overridden via system property MCTELEMETRY_BIND.")
            .define("httpBindAddress", DEFAULT_HTTP_BIND_ADDRESS);

    public static final ModConfigSpec.IntValue TELEMETRY_REFRESH_TICKS = BUILDER
            .comment("Number of server ticks between telemetry JSON refreshes that back the HTTP endpoint.")
            .defineInRange("telemetryRefreshTicks", DEFAULT_REFRESH_TICKS, 1, 12000);

    static final ModConfigSpec SPEC = BUILDER.build();

    private TelemetryConfigNeoForge() {
    }

    public static boolean detailedLoggingEnabled() {
        try {
            return DETAILED_LOGGING.get();
        } catch (IllegalStateException e) {
            if (!detailedLoggingFallbackLogged) {
                detailedLoggingFallbackLogged = true;
                MCTelemetryNeoForge.LOGGER.debug("Detailed logging config not yet loaded; defaulting to false");
            }
            return false;
        }
    }

    public static int httpPort() {
        try {
            return HTTP_PORT.get();
        } catch (IllegalStateException e) {
            MCTelemetryNeoForge.LOGGER.debug("HTTP port config not yet loaded; defaulting to {}", DEFAULT_HTTP_PORT);
            return DEFAULT_HTTP_PORT;
        }
    }

    public static String httpBindAddress() {
        try {
            return HTTP_BIND_ADDRESS.get();
        } catch (IllegalStateException e) {
            MCTelemetryNeoForge.LOGGER.debug("HTTP bind address config not yet loaded; defaulting to {}", DEFAULT_HTTP_BIND_ADDRESS);
            return DEFAULT_HTTP_BIND_ADDRESS;
        }
    }

    public static int telemetryRefreshTicks() {
        try {
            return TELEMETRY_REFRESH_TICKS.get();
        } catch (IllegalStateException e) {
            MCTelemetryNeoForge.LOGGER.debug("Refresh interval config not yet loaded; defaulting to {} ticks", DEFAULT_REFRESH_TICKS);
            return DEFAULT_REFRESH_TICKS;
        }
    }
}
