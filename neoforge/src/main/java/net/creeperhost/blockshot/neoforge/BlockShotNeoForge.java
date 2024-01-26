package net.creeperhost.blockshot.neoforge;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.creeperhost.blockshot.BlockShot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.DistExecutor;
import net.neoforged.fml.common.Mod;

@Mod (BlockShot.MOD_ID)
public class BlockShotNeoForge
{
    public BlockShotNeoForge() {
        // Submit our event bus to let architectury register our content on the right time
        if (Platform.getEnvironment().equals(Env.CLIENT)) {
            BlockShot.init();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> BlockShotNeoForgeClient::init);
        }
    }
}
