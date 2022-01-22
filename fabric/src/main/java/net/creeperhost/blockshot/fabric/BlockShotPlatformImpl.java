package net.creeperhost.blockshot.fabric;

import net.creeperhost.blockshot.BlockShotExpectPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class BlockShotPlatformImpl
{
    /**
     * This is our actual method to {@link BlockShotExpectPlatform#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
