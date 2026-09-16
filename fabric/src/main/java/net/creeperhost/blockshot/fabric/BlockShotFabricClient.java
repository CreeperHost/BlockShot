package net.creeperhost.blockshot.fabric;

import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.capture.RecordingHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public class BlockShotFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(BlockShot.MOD_ID, "recording_overlay"),
                (graphics, deltaTracker) -> RecordingHandler.handleScreenCaptureOverlay(graphics));
    }
}
