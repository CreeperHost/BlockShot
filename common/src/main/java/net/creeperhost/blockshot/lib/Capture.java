package net.creeperhost.blockshot.lib;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.creeperhost.blockshot.WebUtils;


import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Created by brandon3055 on 19/03/2023
 */
public record Capture(String id, NativeImage preview, String format, long created) {
    public static Capture fromJson(JsonObject object) {
        String id = object.get("code").getAsString();
        NativeImage preview = WebUtils.getImageFromUrl("https://blocks.hot/api/v1/shares/" + id + "/preview/smol");
        JsonObject fileMeta = object.get("fileMeta").getAsJsonObject();
        String format = fileMeta.get("type").getAsString();
        OffsetDateTime odt = OffsetDateTime.parse(object.get("created").getAsString());
        long created = odt.toEpochSecond();
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
