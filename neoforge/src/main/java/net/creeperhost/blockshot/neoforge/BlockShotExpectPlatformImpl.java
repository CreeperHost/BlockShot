package net.creeperhost.blockshot.neoforge;

import net.creeperhost.blockshot.BlockShotExpectPlatform;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class BlockShotExpectPlatformImpl
{
    /**
     * This is our actual method to {@link BlockShotExpectPlatform#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
