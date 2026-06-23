package net.creeperhost.blockshot.lib;

import com.mojang.blaze3d.platform.NativeImage;
import net.creeperhost.blockshot.BlockShot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by brandon3055 on 14/09/2023
 */
public class TextureCache {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Identifier FALLBACK_RESOURCE = Identifier.withDefaultNamespace("textures/misc/unknown_server.png");
    private static final Map<String, Identifier> PREVIEW_CACHE = new HashMap<>();
    private static int index = 0;

    public static Identifier loadPreview(Capture capture) {
        return PREVIEW_CACHE.computeIfAbsent(capture.id(), s -> load(capture.preview(), capture.created(), capture.id()));
    }

    public static void unloadPreview(Capture capture) {
        Identifier resource = PREVIEW_CACHE.remove(capture.id());
        if (resource != null) {
            Minecraft.getInstance().getTextureManager().release(resource);
        }
    }

    private static Identifier load(NativeImage nativeImage, long created, String key) {
        try {
            Identifier location = Identifier.fromNamespaceAndPath(BlockShot.MOD_ID, "blockshot/" + index++);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(null, nativeImage));
            return location;
        } catch (Throwable t) {
            LOGGER.warn("An error occurred while loading capture preview", t);
        }
        return FALLBACK_RESOURCE;
    }
}
