package net.creeperhost.blockshot.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.creeperhost.blockshot.capture.RecordingHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Inject(method = "render", at = @At(value = "TAIL"))
    public void render(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        RecordingHandler.handleScreenCapture();
    }

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At (
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    public void render(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci, @Local GuiGraphics guiGraphics) {
        RecordingHandler.handleScreenCaptureOverlay(guiGraphics);
    }
}
