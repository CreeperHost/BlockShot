package net.creeperhost.blockshot.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.ClientUtil;
import net.creeperhost.blockshot.Config;
import net.creeperhost.blockshot.capture.RecordingHandler;
import net.creeperhost.blockshot.capture.ScreenshotHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

import static net.minecraft.client.Screenshot.takeScreenshot;

@Mixin(Screenshot.class)
public abstract class MixinScreenshot {

//    @Shadow
//    public static NativeImage takeScreenshot(RenderTarget arg) {
//        return null;
//    }


    @Inject(method = "_grab", at = @At("HEAD"), cancellable = true)
    private static void takeScreenShot(File file, String string, int width, int height, RenderTarget renderTarget, Consumer<Component> consumer, CallbackInfo ci) {
        if (!BlockShot.isActive()|| !ClientUtil.validState()) {
            return;
        }

        if (RecordingHandler.getEncoder().isWorking() || Screen.hasControlDown()) {
            ci.cancel();
            return;
        }

        if (Config.INSTANCE.uploadMode == Config.Mode.OFF) {
            return;
        }

        try (NativeImage nativeImage = takeScreenshot(width, height, renderTarget)) {
            if (ScreenshotHandler.handleScreenshot(Objects.requireNonNull(nativeImage).asByteArray())) {
                ci.cancel();
            }
        } catch (Throwable e) {
            BlockShot.LOGGER.error("An error occurred while processing screenshot", e);
        }
    }
}
