package net.creeperhost.blockshot.fabric;

import net.creeperhost.blockshot.BlockShot;
import net.fabricmc.api.ModInitializer;

public class BlockShotFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BlockShot.init();
    }
}
