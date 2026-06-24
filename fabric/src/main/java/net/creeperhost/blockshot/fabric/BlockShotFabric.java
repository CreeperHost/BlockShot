package net.creeperhost.blockshot.fabric;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.BlockShotClient;
import net.creeperhost.blockshot.gui.ModTextures;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class BlockShotFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        if (Platform.getEnvironment().equals(Env.CLIENT)) {
            BlockShot.init();
            KeyBindingHelper.registerKeyBinding(BlockShotClient.OPEN_GUI);
            ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
                private final ResourceLocation listenerId = ResourceLocation.fromNamespaceAndPath(BlockShot.MOD_ID, "gui_atlas_reload");

                @Override
                public ResourceLocation getFabricId() {
                    return listenerId;
                }

                @Override
                public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, Executor backgroundExecutor, Executor gameExecutor) {
                    return ModTextures.getAtlasHolder().reload(preparationBarrier, resourceManager, backgroundExecutor, gameExecutor);
                }
            });
        }
    }
}
