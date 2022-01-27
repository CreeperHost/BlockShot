package net.creeperhost.blockshot.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.BlockShotClickEvent;
import net.creeperhost.blockshot.WebUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Base64;

@Mixin(Screen.class)
public abstract class MixinScreen {
    @Inject(method = "handleComponentClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;hasShiftDown()Z"), cancellable = true)
    public void handleComponentClicked(Style _style, CallbackInfoReturnable<Boolean> cir)
    {
        if (!Screen.hasShiftDown()) {
            ClickEvent clickEvent = _style.getClickEvent();
            if (clickEvent == null) return;
            if (clickEvent instanceof BlockShotClickEvent) {
                cir.cancel();
                if (BlockShot.latest == null || BlockShot.latest.length == 0) {
                    cir.setReturnValue(true);
                    return;
                }
                Util.ioPool().execute(() ->
                {
                    ((MixinChatComponent) Minecraft.getInstance().gui.getChat()).invokeremoveById(BlockShot.BLOCKSHOT_UPLOAD_ID);
                    try {
                        byte[] bytes = BlockShot.latest;
                        try {
                            String result = BlockShot.uploadImage(bytes);
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
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (Exception var7) {
                        TranslatableComponent err = new TranslatableComponent("Failed to create screenshot ", new Object[]{var7.getMessage()});
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.sendMessage(err, Util.NIL_UUID);
                        }
                    }
                    BlockShot.latest = null;
                });
                cir.setReturnValue(true);
            }
        }
    }
}
