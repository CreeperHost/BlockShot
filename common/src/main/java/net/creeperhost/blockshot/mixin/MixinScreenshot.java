package net.creeperhost.blockshot.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.creeperhost.blockshot.Config;
import net.creeperhost.blockshot.WebUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
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
        if(Config.INSTANCE.enabled)
        {
            NativeImage nativeImage = takeScreenshot(renderTarget);

            Util.ioPool().execute(() ->
            {
                try
                {
                    byte[] bytes = nativeImage.asByteArray();
                    try
                    {
                        String rsp = WebUtils.putWebResponse("https://blockshot.ch/upload", Base64.getEncoder().encodeToString(bytes), false, false);
                        JsonElement jsonElement = new JsonParser().parse(rsp);
                        String status = jsonElement.getAsJsonObject().get("status").getAsString();
                        if (!status.equals("error"))
                        {
                            String url = jsonElement.getAsJsonObject().get("url").getAsString();
                            Component link = (new TextComponent(url)).withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
                            consumer.accept(link);
                        }
                        else
                        {
                            String message = jsonElement.getAsJsonObject().get("message").getAsString();
                            Component failMessage = new TextComponent(message);
                            consumer.accept(failMessage);
                        }
                        ci.cancel();
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                } catch (Exception var7)
                {
                    LOGGER.warn("Couldn't save screenshot", var7);
                    consumer.accept(new TranslatableComponent("Failed to create screenshot ", new Object[]{var7.getMessage()}));
                } finally
                {
                    nativeImage.close();
                }
            });
            ci.cancel();
        }
    }
}
