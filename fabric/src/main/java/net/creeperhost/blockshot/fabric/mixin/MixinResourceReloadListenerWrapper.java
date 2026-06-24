package net.creeperhost.blockshot.fabric.mixin;

import net.creeperhost.polylib.client.modulargui.sprite.ModAtlasHolder;
import net.creeperhost.polylib.fabric.client.ResourceReloadListenerWrapper;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Mixin(value = ResourceReloadListenerWrapper.class, remap = false)
public class MixinResourceReloadListenerWrapper {
    @Shadow
    private Supplier<ModAtlasHolder> getWrapped;

    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, Executor backgroundExecutor, Executor gameExecutor) {
        return getWrapped.get().reload(preparationBarrier, resourceManager, backgroundExecutor, gameExecutor);
    }
}
