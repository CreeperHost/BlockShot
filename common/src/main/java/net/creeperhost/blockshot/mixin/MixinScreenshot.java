package net.creeperhost.blockshot.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.BlockShotClickEvent;
import net.creeperhost.blockshot.Config;
import net.creeperhost.blockshot.WebUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public abstract class MixinScreenshot
{
    @Shadow @Final private static Logger LOGGER;

    @Shadow public static NativeImage takeScreenshot(RenderTarget arg) {return null;}

    @Inject(method = "_grab", at = @At("HEAD"), cancellable = true)
    private static void takeScreenShot(File file, String string, RenderTarget renderTarget, Consumer<Component> consumer, CallbackInfo ci)
    {
        if(BlockShot.isRecording || Screen.hasControlDown())
        {
            ci.cancel();
            return;
        }
        if(Config.INSTANCE.uploadMode != 0)
        {
            NativeImage nativeImage = takeScreenshot(renderTarget);
            if(nativeImage == null) return;
            byte[] bytes = new byte[0];
            try {
                bytes = nativeImage.asByteArray();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            } finally
            {
                nativeImage.close();
            }
            BlockShot.latest = bytes;
            if(Config.INSTANCE.uploadMode == 2) {
                Util.ioPool().execute(() ->
                {
                    try {
                        try {
                            if(BlockShot.latest == null || BlockShot.latest.length == 0) return;

                            String result = BlockShot.uploadImage(BlockShot.latest);
                            if(result == null)
                            {
                                if(Minecraft.getInstance() != null && Minecraft.getInstance().gui.getChat() != null) {
                                    Component finished = new TextComponent("An error occurred uploading your content to BlockShot.");
                                    ((MixinChatComponent)Minecraft.getInstance().gui.getChat()).invokeaddMessage(finished, BlockShot.BLOCKSHOT_UPLOAD_ID);
                                }
                            } else if(result.startsWith("http"))
                            {
                                Component link = (new TextComponent(result)).withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, result)));
                                Component finished = new TextComponent("Your content is now available on BlockShot! ").append(link);
                                ((MixinChatComponent)Minecraft.getInstance().gui.getChat()).invokeremoveById(BlockShot.BLOCKSHOT_UPLOAD_ID);
                                Minecraft.getInstance().gui.getChat().addMessage(finished);
                            }
                            BlockShot.latest = null;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (Exception var7) {
                        LOGGER.warn("Couldn't save screenshot", var7);
                        consumer.accept(new TranslatableComponent("Failed to create screenshot ", new Object[]{var7.getMessage()}));
                    }
                });
            } else {
                if(BlockShot.latest != null && BlockShot.latest.length > 0)
                {
                    TextComponent confirmMessage = new TextComponent("Click here to upload this screenshot to BlockShot");
                    confirmMessage.setStyle(confirmMessage.getStyle().withClickEvent(new BlockShotClickEvent(ClickEvent.Action.RUN_COMMAND, "/blockshot upload")));
                    ((MixinChatComponent) Minecraft.getInstance().gui.getChat()).invokeaddMessage(confirmMessage, BlockShot.BLOCKSHOT_UPLOAD_ID);
                }
            }
            ci.cancel();
        }
    }
}
