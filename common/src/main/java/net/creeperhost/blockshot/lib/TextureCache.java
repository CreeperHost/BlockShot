package net.creeperhost.blockshot.lib;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by brandon3055 on 14/09/2023
 */
public class TextureCache {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation FALLBACK_RESOURCE = new ResourceLocation("textures/misc/unknown_server.png");
    private static final Map<String, ResourceLocation> PREVIEW_CACHE = new HashMap<>();

    public static ResourceLocation loadPreview(Capture capture) {
        return PREVIEW_CACHE.computeIfAbsent(capture.id(), s -> load(capture.preview(), capture.created()));
    }

    public static void unloadPreview(Capture capture) {
        ResourceLocation resource = PREVIEW_CACHE.remove(capture.id());
        if (resource != null) {
            Minecraft.getInstance().getTextureManager().release(resource);
        }
    }

    private static ResourceLocation load(String base64Texture, long created) {
        try {
            if (StringUtils.isNotBlank(base64Texture) && created > 0) {
                byte[] bs = Base64.getDecoder().decode(base64Texture.replaceAll("\n", "").getBytes(StandardCharsets.UTF_8));
                return Minecraft.getInstance().getTextureManager().register("blockshot/", new DynamicTexture(NativeImage.read(new ByteArrayInputStream(bs))));
            }
        } catch (Throwable t) {
            LOGGER.warn("An error occurred while loading capture preview", t);
        }
        return FALLBACK_RESOURCE;
    }
}
