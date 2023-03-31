package net.creeperhost.blockshot.mixin;

import net.creeperhost.blockshot.capture.RecordingHandler;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Inject(method = "render", at = @At(value = "TAIL"), cancellable = false)
    public void render(float f, long l, boolean bl, CallbackInfo ci) {
        RecordingHandler.handleScreenCapture();
    }
}
