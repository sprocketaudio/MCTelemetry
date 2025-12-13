package net.sprocketgames.mctelemetry.common;

import java.util.Objects;

/**
 * Simple carrier for player identity used by telemetry payloads.
 */
public record PlayerSnapshot(String name, String uuid) {
    public PlayerSnapshot {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(uuid, "uuid");
    }
}
