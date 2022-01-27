package net.creeperhost.blockshot.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.GifEncoder;
import net.creeperhost.blockshot.gui.BlockShotClickEvent;
import net.creeperhost.blockshot.Config;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public abstract class MixinScreenshot
{
    @Shadow public static NativeImage takeScreenshot(RenderTarget arg) {return null;}

    @Inject(method = "_grab", at = @At("HEAD"), cancellable = true)
    private static void takeScreenShot(File file, String string, RenderTarget renderTarget, Consumer<Component> consumer, CallbackInfo ci)
    {
        if(BlockShot.isActive()) {
            if (GifEncoder.isRecording || Screen.hasControlDown()) {
                ci.cancel();
                return;
            }
            if (Config.INSTANCE.uploadMode != 0) {
                NativeImage nativeImage = takeScreenshot(renderTarget);
                if (nativeImage == null) return;
                byte[] bytes;
                try {
                    bytes = nativeImage.asByteArray();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } finally {
                    nativeImage.close();
                }
                BlockShot.latest = bytes;
                if (Config.INSTANCE.uploadMode == 2) {
                    Util.ioPool().execute(() ->
                    {
                        if (BlockShot.latest == null || BlockShot.latest.length == 0) return;
                        BlockShot.uploadAndAddToChat(BlockShot.latest);
                        BlockShot.latest = null;
                    });
                } else {
                    if (BlockShot.latest != null && BlockShot.latest.length > 0) {
                        TextComponent confirmMessage = new TextComponent("Click here to upload this screenshot to BlockShot");
                        confirmMessage.setStyle(confirmMessage.getStyle().withClickEvent(new BlockShotClickEvent(ClickEvent.Action.RUN_COMMAND, "/blockshot upload")));
                        if(Minecraft.getInstance() != null && Minecraft.getInstance().gui.getChat() != null) {
                            ((MixinChatComponent) Minecraft.getInstance().gui.getChat()).invokeaddMessage(confirmMessage, BlockShot.CHAT_UPLOAD_ID);
                        }
                    }
                }
                ci.cancel();
            }
        }
    }
}
