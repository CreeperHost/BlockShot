package net.creeperhost.blockshot.mixin;

import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.gui.GuiEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreen {
    @Inject(method = "defaultHandleGameClickEvent", at = @At("HEAD"), cancellable = true)
    private static void handleGameClickEvent(ClickEvent clickEvent, Minecraft minecraft, Screen screen, CallbackInfo ci) {
        handleBlockShotClick(clickEvent, ci);
    }

    @Inject(method = "defaultHandleClickEvent", at = @At("HEAD"), cancellable = true)
    private static void handleClickEvent(ClickEvent clickEvent, Minecraft minecraft, Screen screen, CallbackInfo ci) {
        handleBlockShotClick(clickEvent, ci);
    }

    private static void handleBlockShotClick(ClickEvent clickEvent, CallbackInfo ci) {
        if (BlockShot.isActive()) {
            if (GuiEvents.handleClickEvent(clickEvent)) {
                ci.cancel();
            }
        }
    }
}
