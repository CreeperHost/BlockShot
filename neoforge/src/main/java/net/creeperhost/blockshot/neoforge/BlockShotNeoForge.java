package net.creeperhost.blockshot.neoforge;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.creeperhost.blockshot.BlockShot;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod (BlockShot.MOD_ID)
public class BlockShotNeoForge {
    public BlockShotNeoForge(IEventBus modBus) {
        if (Platform.getEnvironment().equals(Env.CLIENT)) {
            BlockShot.init();
            BlockShotNeoForgeClient.init(modBus);
        }
    }
}
