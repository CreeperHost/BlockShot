package net.creeperhost.blockshot.lib;

import com.mojang.blaze3d.platform.NativeImage;
import net.creeperhost.blockshot.BlockShot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by brandon3055 on 14/09/2023
 */
public class TextureCache {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation FALLBACK_RESOURCE = ResourceLocation.withDefaultNamespace("textures/misc/unknown_server.png");
    private static final Map<String, ResourceLocation> PREVIEW_CACHE = new HashMap<>();
    private static int index = 0;

    public static ResourceLocation loadPreview(Capture capture) {
        return PREVIEW_CACHE.computeIfAbsent(capture.id(), s -> load(capture.preview(), capture.created()));
    }

    public static void unloadPreview(Capture capture) {
        ResourceLocation resource = PREVIEW_CACHE.remove(capture.id());
        if (resource != null) {
            Minecraft.getInstance().getTextureManager().release(resource);
        }
    }

    private static ResourceLocation load(NativeImage nativeImage, long created) {
        try {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(BlockShot.MOD_ID, "blockshot/" + index++);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(null));
            return location;
        } catch (Throwable t) {
            LOGGER.warn("An error occurred while loading capture preview", t);
        }
        return FALLBACK_RESOURCE;
    }
}
