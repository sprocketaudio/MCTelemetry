package net.sprocketgames.mctelemetry.common;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of telemetry data at a moment in time.
 */
public record TelemetrySnapshot(String minecraftVersion, String loader, List<PlayerSnapshot> players, Double mspt, Double tps) {
    public TelemetrySnapshot {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(players, "players");
    }

    public static TelemetrySnapshot of(String minecraftVersion, String loader, Collection<PlayerSnapshot> players, Double mspt, Double tps) {
        Objects.requireNonNull(players, "players");
        return new TelemetrySnapshot(minecraftVersion, loader, List.copyOf(players), mspt, tps);
    }
}
