package net.sprocketgames.mctelemetry.common.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lightweight HTTP server that exposes cached telemetry JSON to localhost only.
 */
public class TelemetryHttpServer {
    private static final int DEFAULT_PORT = 8765;
    private static final String DEFAULT_BIND_ADDRESS = "127.0.0.1";
    private static final String BIND_OVERRIDE_PROPERTY = "MCTELEMETRY_BIND";

    private final Logger logger;
    private final AtomicReference<String> lastTelemetryJson;
    private final int port;
    private final InetAddress bindAddress;
    private final String bindAddressText;

    private HttpServer server;

    public TelemetryHttpServer(Logger logger, String initialTelemetry) {
        this(logger, initialTelemetry, resolvePort(DEFAULT_PORT), DEFAULT_BIND_ADDRESS);
    }

    public TelemetryHttpServer(Logger logger, String initialTelemetry, int port, String bindAddress) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.lastTelemetryJson = new AtomicReference<>(initialTelemetry == null ? "{}" : initialTelemetry);
        this.port = port > 0 ? port : DEFAULT_PORT;
        InetAddress resolved = resolveBindAddress(logger, bindAddress);
        this.bindAddress = resolved;
        this.bindAddressText = resolved.getHostAddress();
    }

    static int resolvePort(int configuredPort) {
        String property = System.getProperty("MCTELEMETRY_PORT");
        if (property == null || property.isBlank()) {
            return validatePort(configuredPort) ? configuredPort : DEFAULT_PORT;
        }

        try {
            int parsed = Integer.parseInt(property.trim());
            return validatePort(parsed) ? parsed : DEFAULT_PORT;
        } catch (NumberFormatException ignored) {
            // fall through to default
        }

        return DEFAULT_PORT;
    }

    private static boolean validatePort(int candidate) {
        return candidate > 0 && candidate <= 65535;
    }

    private static InetAddress resolveBindAddress(Logger logger, String configuredAddress) {
        String override = System.getProperty(BIND_OVERRIDE_PROPERTY);
        String desired = override != null && !override.isBlank() ? override.trim() : configuredAddress;
        if (desired == null || desired.isBlank()) {
            desired = DEFAULT_BIND_ADDRESS;
        } else {
            desired = desired.trim();
        }

        try {
            InetAddress resolved = InetAddress.getByName(desired);
            if (!(resolved.isLoopbackAddress() || resolved.isAnyLocalAddress())) {
                throw new IllegalArgumentException("Bind address must be loopback or 0.0.0.0: " + desired);
            }

            return resolved;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to resolve bind address: " + desired, e);
        }
    }

    boolean start() {
        if (server != null) {
            return true;
        }

        try {
            InetSocketAddress address = new InetSocketAddress(bindAddress, port);
            server = HttpServer.create(address, 0);
            server.createContext("/telemetry", new TelemetryHandler());
            server.createContext("/health", new HealthHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            return true;
        } catch (IOException e) {
            logger.error("Failed to start telemetry HTTP endpoint", e);
            server = null;
            return false;
        }
    }

    void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            logger.info("Stopped telemetry HTTP endpoint");
        }
    }

    void updateTelemetry(String telemetryJson) {
        if (telemetryJson != null && !telemetryJson.isBlank()) {
            lastTelemetryJson.set(telemetryJson);
        }
    }

    String bindAddress() {
        return bindAddressText;
    }

    private final class TelemetryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().add("Allow", "GET");
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                byte[] payload = lastTelemetryJson.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, payload.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(payload);
                }
            } finally {
                exchange.close();
            }
        }
    }

    private final class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().add("Allow", "GET");
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                byte[] payload = "ok".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(200, payload.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(payload);
                }
            } finally {
                exchange.close();
            }
        }
    }
}
