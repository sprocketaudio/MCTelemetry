package net.sprocketgames.mctelemetry.common;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.Objects;

public final class TelemetryPayload {
    private static final Gson GSON = new Gson();

    private TelemetryPayload() {
    }

    public static String build(TelemetrySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return build(snapshot.minecraftVersion(), snapshot.loader(), snapshot.players(), snapshot.mspt(), snapshot.tps());
    }

    public static String build(String minecraftVersion, String loader, Collection<PlayerSnapshot> players) {
        return build(minecraftVersion, loader, players, null, null);
    }

    public static String build(String minecraftVersion, String loader, Collection<PlayerSnapshot> players, Double mspt, Double tps) {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(players, "players");

        JsonObject root = new JsonObject();
        root.addProperty("mc", minecraftVersion);
        root.addProperty("loader", loader);

        addNullableNumber(root, "mspt", mspt);
        addNullableNumber(root, "tps", tps);

        JsonArray playersArray = new JsonArray();
        for (PlayerSnapshot player : players) {
            JsonObject playerObj = new JsonObject();
            playerObj.addProperty("name", player.name());
            playerObj.addProperty("uuid", player.uuid());
            playersArray.add(playerObj);
        }

        root.add("players", playersArray);
        return GSON.toJson(root);
    }

    private static void addNullableNumber(JsonObject root, String key, Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            root.add(key, JsonNull.INSTANCE);
            return;
        }

        root.addProperty(key, value);
    }
}
