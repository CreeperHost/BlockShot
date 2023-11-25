package net.creeperhost.blockshot.lib;

import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * Created by brandon3055 on 19/03/2023
 */
public record Capture(String id, String preview, String format, long created) {
    public static Capture fromJson(JsonObject object) {
        String id = object.get("id").getAsString();
        String preview = object.get("preview").getAsString();
        String format = object.get("format").getAsString();
        long created = object.get("created").getAsLong();
        return new Capture(id, preview, format, created);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Capture capture = (Capture) o;
        return Objects.equals(id, capture.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
