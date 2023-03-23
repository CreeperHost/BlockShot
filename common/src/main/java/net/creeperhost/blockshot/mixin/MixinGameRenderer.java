package net.creeperhost.blockshot.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.GifEncoder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Inject(method = "render", at = @At(value = "TAIL"), cancellable = false)
    public void render(float f, long l, boolean bl, CallbackInfo ci) {
        if (!GifEncoder.isRecording || !BlockShot.isActive()) {
            return;
        }

        int skipFrames = 6;
        if (BlockShot.getFPS() > 20) {
            skipFrames = (BlockShot.getFPS() / 10);
        }
        if (GifEncoder.frames > skipFrames || (GifEncoder.lastTimestamp != (System.currentTimeMillis() / 1000))) {
            GifEncoder.frames = 0;
            if (GifEncoder.lastTimestamp != (System.currentTimeMillis() / 1000)) {
                GifEncoder.lastTimestamp = (System.currentTimeMillis() / 1000);
                GifEncoder.totalSeconds++;
            }
            RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
            NativeImage nativeImage = new NativeImage(renderTarget.width, renderTarget.height, false);
            RenderSystem.bindTexture(renderTarget.getColorTextureId());
            nativeImage.downloadTexture(0, true);
            GifEncoder.addFrameAndClose(nativeImage);
            if (GifEncoder.totalSeconds > 30) GifEncoder.isRecording = false;
        } else {
            GifEncoder.frames++;
        }
    }
}
