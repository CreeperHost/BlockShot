package net.creeperhost.blockshot.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.creeperhost.blockshot.*;
import net.creeperhost.blockshot.gui.BlockShotClickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public abstract class MixinScreenshot {
    @Shadow
    public static NativeImage takeScreenshot(RenderTarget arg) {
        return null;
    }

    @Inject(method = "_grab", at = @At("HEAD"), cancellable = true)
    private static void takeScreenShot(File file, String string, RenderTarget renderTarget, Consumer<Component> consumer, CallbackInfo ci) {
        if (!BlockShot.isActive() || Config.INSTANCE.uploadMode == Config.Mode.OFF || !ClientUtil.validState()) {
            return;
        }

        if (GifEncoder.isRecording || Screen.hasControlDown()) {
            ci.cancel();
            return;
        }

        try (NativeImage nativeImage = takeScreenshot(renderTarget)) {
            if (ScreenshotHandler.handleScreenshot(Objects.requireNonNull(nativeImage).asByteArray())) {
                ci.cancel();
            }
        } catch (Throwable e) {
            BlockShot.LOGGER.error("An error occurred while processing screenshot", e);
        }
    }
}
