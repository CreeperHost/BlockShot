package net.creeperhost.blockshot.mixin;

import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.gui.BlockShotClickEvent;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class MixinScreen {
    @Inject(method = "handleComponentClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;hasShiftDown()Z"), cancellable = true)
    public void handleComponentClicked(Style _style, CallbackInfoReturnable<Boolean> cir) {
        if (BlockShot.isActive()) {
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
                        if (Minecraft.getInstance() != null && Minecraft.getInstance().gui.getChat() != null) {
                            byte[] bytes = BlockShot.latest;
                            BlockShot.uploadAndAddToChat(bytes);
                        }
                        BlockShot.latest = null;
                    });
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
