package net.creeperhost.blockshot.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.creeperhost.blockshot.BlockShot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"), cancellable = false)
    public void render(float f, long l, boolean bl, CallbackInfo ci) {
        if(BlockShot.isRecording)
        {
            if(BlockShot.lastTimestamp == Instant.now().getEpochSecond())
            {
                BlockShot.curfps++;
            } else {
                BlockShot.lastfps = BlockShot.curfps;
                BlockShot.curfps = 0;
            }
            int skipFrames = 6;
            if(BlockShot.lastfps > 20) {
                skipFrames = (int) (BlockShot.lastfps / 10);
            }
            if(BlockShot.frames > skipFrames || (BlockShot.lastTimestamp != Instant.now().getEpochSecond())) {
                BlockShot.frames = 0;
                if (BlockShot.lastTimestamp != Instant.now().getEpochSecond()) {
                    BlockShot.lastTimestamp = Instant.now().getEpochSecond();
                    BlockShot.totalSeconds++;
                }
                RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
                NativeImage nativeImage = new NativeImage(renderTarget.width, renderTarget.height, false);
                RenderSystem.bindTexture(renderTarget.getColorTextureId());
                nativeImage.downloadTexture(0, true);
                BlockShot.addFrameAndClose(nativeImage);
                if(BlockShot.totalSeconds > 30) BlockShot.isRecording = false;
            } else {
                BlockShot.frames++;
            }
        }
    }
}
