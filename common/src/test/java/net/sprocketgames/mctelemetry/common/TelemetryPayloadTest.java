package net.sprocketgames.mctelemetry.common;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryPayloadTest {
    private static final Gson GSON = new Gson();

    @Test
    void payloadIncludesNullMsptAndTpsWhenUnavailable() {
        String payload = TelemetryPayload.build("1.20.1", "forge", Collections.emptyList());

        JsonObject root = GSON.fromJson(payload, JsonObject.class);
        assertTrue(root.has("mspt"), "mspt key should always be present");
        assertTrue(root.has("tps"), "tps key should always be present");

        JsonElement mspt = root.get("mspt");
        JsonElement tps = root.get("tps");
        assertTrue(mspt.isJsonNull(), "mspt should be null when unavailable");
        assertTrue(tps.isJsonNull(), "tps should be null when unavailable");
    }

    @Test
    void payloadIncludesMsptAndTpsWhenProvided() {
        String payload = TelemetryPayload.build("1.20.1", "forge", Collections.emptyList(), 50.0, 20.0);

        JsonObject root = GSON.fromJson(payload, JsonObject.class);
        assertEquals(50.0, root.get("mspt").getAsDouble());
        assertEquals(20.0, root.get("tps").getAsDouble());
    }
}
