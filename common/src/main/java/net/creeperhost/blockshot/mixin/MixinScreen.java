package net.creeperhost.blockshot.mixin;

import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.gui.BlockShotClickEvent;
import net.creeperhost.blockshot.gui.GuiEvents;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class MixinScreen {
    @Shadow public abstract boolean handleComponentClicked(@Nullable Style style);

    @Inject(method = "handleComponentClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;hasShiftDown()Z"), cancellable = true)
    public void handleComponentClicked(Style _style, CallbackInfoReturnable<Boolean> cir) {
        if (BlockShot.isActive()) {
            if(_style == null) return;
            if (GuiEvents.handleComponentClick(_style)) {
                cir.cancel();
                cir.setReturnValue(true);
            }
        }
    }
}
