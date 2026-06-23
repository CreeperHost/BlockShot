package net.creeperhost.blockshot.forge;

import net.creeperhost.blockshot.BlockShotClient;
import net.minecraftforge.client.ClientRegistry;

public class BlockShotForgeClient {
    public static void init() {
        ClientRegistry.registerKeyBinding(BlockShotClient.OPEN_GUI);
    }
}
