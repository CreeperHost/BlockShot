package net.creeperhost.blockshot.neoforge;

import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.BlockShotClient;
import net.creeperhost.blockshot.gui.ModTextures;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public class BlockShotNeoForgeClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(BlockShotNeoForgeClient::registerKeyMappings);
        modBus.addListener(BlockShotNeoForgeClient::registerReloadListeners);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(BlockShotClient.OPEN_GUI);
    }

    private static void registerReloadListeners(AddClientReloadListenersEvent event)
    {
        event.addListener(ResourceLocation.fromNamespaceAndPath(BlockShot.MOD_ID, "textures_reload"), ModTextures.getAtlasHolder());
    }
}
