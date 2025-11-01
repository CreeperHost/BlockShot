package net.creeperhost.blockshot.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.ClientUtil;
import net.creeperhost.blockshot.Config;
import net.creeperhost.blockshot.capture.RecordingHandler;
import net.creeperhost.blockshot.capture.ScreenshotHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public abstract class MixinScreenshot {

    @Shadow
    public static void takeScreenshot(RenderTarget renderTarget, Consumer<NativeImage> consumer) {}

    @Inject(method = "grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V", at = @At("HEAD"), cancellable = true)
    private static void takeScreenShot(File file, RenderTarget renderTarget, Consumer<Component> consumer, CallbackInfo ci) {
        if (!BlockShot.isActive() || !ClientUtil.validState()) {
            return;
        }

        if (RecordingHandler.getEncoder().isWorking() || Minecraft.getInstance().hasControlDown()) {
            ci.cancel();
            return;
        }

        if (Config.INSTANCE.uploadMode == Config.Mode.OFF) {
            return;
        }

        takeScreenshot(renderTarget, image -> {
            try (image) {
                ScreenshotHandler.handleScreenshot(ClientUtil.nativeImageBytes(Objects.requireNonNull(image)));
            } catch (Throwable e) {
                BlockShot.LOGGER.error("An error occurred while processing screenshot", e);
            }
        });
        ci.cancel(); //This is problematic, we need to not cancel if handleScreenshot fails, but i don't know where that lambda is handled. If it's a delayed execution then that wont work.

//        try (NativeImage nativeImage = takeScreenshot(renderTarget)) {
//            if (ScreenshotHandler.handleScreenshot(ClientUtil.nativeImageBytes(Objects.requireNonNull(nativeImage)))) {
//                ci.cancel();
//            }
//        } catch (Throwable e) {
//            BlockShot.LOGGER.error("An error occurred while processing screenshot", e);
//        }
    }
}
