package net.creeperhost.blockshot.neoforge;

import net.creeperhost.blockshot.BlockShotClient;
import net.creeperhost.blockshot.gui.ModTextures;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public class BlockShotNeoForgeClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(BlockShotNeoForgeClient::registerKeyMappings);
        modBus.addListener(BlockShotNeoForgeClient::registerReloadListeners);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(BlockShotClient.OPEN_GUI);
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event)
    {
        event.registerReloadListener(ModTextures.getAtlasHolder());
    }
}
