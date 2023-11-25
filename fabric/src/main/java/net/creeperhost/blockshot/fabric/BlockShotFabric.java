package net.creeperhost.blockshot.fabric;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.BlockShotClient;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class BlockShotFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        if (Platform.getEnvironment().equals(Env.CLIENT)) {
            BlockShot.init();
            KeyBindingHelper.registerKeyBinding(BlockShotClient.OPEN_GUI);
        }
    }
}
