package net.creeperhost.blockshot.neoforge;

import net.creeperhost.blockshot.BlockShotClient;
import net.creeperhost.blockshot.capture.RecordingHandler;
import net.creeperhost.blockshot.gui.ModTextures;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterTextureAtlasesEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.common.NeoForge;

public class BlockShotNeoForgeClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(BlockShotNeoForgeClient::registerKeyMappings);
        modBus.addListener(BlockShotNeoForgeClient::atlasStitched);
        modBus.addListener(BlockShotNeoForgeClient::registerTextureAtlas);
        NeoForge.EVENT_BUS.addListener(BlockShotNeoForgeClient::renderRecordingOverlay);
    }

    private static void renderRecordingOverlay(RenderGuiEvent.Post event) {
        RecordingHandler.handleScreenCaptureOverlay(event.getGuiGraphics());
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
