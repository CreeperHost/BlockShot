package net.creeperhost.blockshot.gui;

import net.creeperhost.blockshot.Config;
import net.creeperhost.blockshot.capture.RecordingHandler;
import net.creeperhost.blockshot.capture.ScreenshotHandler;
import net.creeperhost.polylib.client.modulargui.ModularGuiInjector;
import net.creeperhost.polylib.client.modulargui.ModularGuiScreen;
import net.creeperhost.polylib.event.events.client.PolyInputEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;

/**
 * Created by brandon3055 on 18/03/2023
 */
public class GuiEvents {

    private static long keybindLast = 0;

    public static void init() {
        PolyInputEvents.INPUT_KEY.register(GuiEvents::onRawInput);
        ModularGuiInjector.registerInjection(screen -> screen instanceof PauseScreen, screen -> new PauseScreenGuiInjection());
    }

    private static void onRawInput(int key, int scanCode, int action, int modifiers) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.keyScreenshot.matches(new KeyEvent(key, scanCode, modifiers)) || action != 0) { //Have to use key release because key pressed does not get fired on forge for keyScreenshot.
            return;
        }

        long elapsed = System.currentTimeMillis() - keybindLast;
        if (elapsed < 5000 && !RecordingHandler.getEncoder().isWorking()) {
            return;
        }
        keybindLast = System.currentTimeMillis();

        if (Minecraft.getInstance().hasControlDown()) {
            RecordingHandler.getEncoder().startOrStopRecording();
        } else if (Minecraft.getInstance().hasShiftDown()) {
            RecordingHandler.getEncoder().cancelRecording();
        }
    }

    public static boolean handleComponentClick(Style style) {
        if (style == null) return false;
        return handleClickEvent(style.getClickEvent());
    }

    public static boolean handleClickEvent(ClickEvent clickEvent) {
        if (Minecraft.getInstance().hasShiftDown()) return false;
        if (!(clickEvent instanceof BlockShotUploadEvent)) return false;

        //If we fail to upload here there is no need to write to disk because this image is already on disk.
        ScreenshotHandler.uploadLast(false);
        return true;
    }
}
