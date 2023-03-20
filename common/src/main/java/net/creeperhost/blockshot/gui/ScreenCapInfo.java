package net.creeperhost.blockshot.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;

/**
 * Created by brandon3055 on 19/03/2023
 */
public class ScreenCapInfo {
    public final String id;
    public final String preview;
    public final long created;
    public boolean deleting;
    public String error;

    public ScreenCapInfo(JsonObject object) {
        id = object.get("id").getAsString();
        preview = object.get("preview").getAsString();
        created = object.get("created").getAsLong();
    }

    public ScreenCapInfo(String id, String preview, long created, boolean isDeleting) {
        this.id = id;
        this.preview = preview;
        this.created = created;
        this.deleting = isDeleting;
    }

    public ScreenCapInfo(String error) {
        this("", "", 0, false);
        this.error = error;
    }

    public String getDisplay() {
        if (error != null) return ChatFormatting.RED + error;
        return deleting ? I18n.get("gui.blockshot.history.delete.pending") : "https://blockshot.ch/" + id;
    }
}
