package net.creeperhost.blockshot.mixin;

import net.creeperhost.blockshot.capture.RecordingHandler;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Inject(method = "render()V", at = @At(value = "TAIL"))
    public void render(CallbackInfo ci) {
        RecordingHandler.handleScreenCapture();
    }
}
