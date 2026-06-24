package net.creeperhost.blockshot.neoforge;

import net.creeperhost.blockshot.BlockShot;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;

@Mod (BlockShot.MOD_ID)
public class BlockShotNeoForge {
    public BlockShotNeoForge(IEventBus modBus) {
        if (FMLLoader.getCurrent().getDist().isClient()) {
            BlockShot.init();
            BlockShotNeoForgeClient.init(modBus);
        }
    }
}
