package net.creeperhost.blockshot.forge;

import dev.architectury.platform.Platform;
import dev.architectury.platform.forge.EventBuses;
import dev.architectury.utils.Env;
import net.creeperhost.blockshot.BlockShot;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BlockShot.MOD_ID)
public class BlockShotForge
{
    public BlockShotForge() {
        // Submit our event bus to let architectury register our content on the right time
        if (Platform.getEnvironment().equals(Env.CLIENT)) {
            EventBuses.registerModEventBus(BlockShot.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
            BlockShot.init();
        }
    }
}
