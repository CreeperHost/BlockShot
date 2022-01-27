package net.creeperhost.blockshot.fabric;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.creeperhost.blockshot.BlockShot;
import net.fabricmc.api.ModInitializer;

public class BlockShotFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        if (Platform.getEnvironment().equals(Env.CLIENT)) {
            BlockShot.init();
        }
    }
}
