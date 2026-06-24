package net.creeperhost.blockshot.neoforge;

import net.creeperhost.blockshot.BlockShotClient;
import net.creeperhost.blockshot.gui.ModTextures;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterTextureAtlasesEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

public class BlockShotNeoForgeClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(BlockShotNeoForgeClient::registerKeyMappings);
        modBus.addListener(BlockShotNeoForgeClient::atlasStitched);
        modBus.addListener(BlockShotNeoForgeClient::registerTextureAtlas);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(BlockShotClient.OPEN_GUI);
    }

    private static void registerTextureAtlas(RegisterTextureAtlasesEvent event) {
        AtlasManager.AtlasConfig config = new AtlasManager.AtlasConfig(ModTextures.TEXTURE_ID, ModTextures.DEFINITION_LOCATION, false);
        event.register(config);
    }

    private static void atlasStitched(TextureAtlasStitchedEvent event) {
        if (event.getAtlas().location().equals(ModTextures.TEXTURE_ID)) {
            ModTextures.setAtlas(event.getAtlas());
        }
    }
}
