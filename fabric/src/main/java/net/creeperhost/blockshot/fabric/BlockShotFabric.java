package net.creeperhost.blockshot.fabric;

import net.creeperhost.blockshot.BlockShot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class BlockShotFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            BlockShot.init();
//            ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new ResourceReloadListenerWrapper(ModTextures::getAtlasHolder, ResourceLocation.fromNamespaceAndPath(BlockShot.MOD_ID, "gui_atlas_reload")));
        }
    }
}
