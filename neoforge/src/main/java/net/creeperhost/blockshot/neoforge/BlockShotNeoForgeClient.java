package net.creeperhost.blockshot.neoforge;

import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.BlockShotClient;
import net.creeperhost.blockshot.gui.ModTextures;
import net.creeperhost.blockshot.integration.Integration;
import net.creeperhost.blockshot.neoforge.compat.PauseMenuIntegration;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public class BlockShotNeoForgeClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(BlockShotNeoForgeClient::registerKeyMappings);
        modBus.addListener(BlockShotNeoForgeClient::registerReloadListeners);
        Integration.runOptional("ftbpmapi", () -> PauseMenuIntegration::init);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(BlockShotClient.OPEN_GUI);
    }

    private static void registerReloadListeners(AddClientReloadListenersEvent event)
    {
        event.addListener(ResourceLocation.fromNamespaceAndPath(BlockShot.MOD_ID, "gui_atlas_reload"), ModTextures.getAtlasHolder());
    }
}
