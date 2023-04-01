package net.creeperhost.blockshot.gui;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.hooks.client.screen.ScreenAccess;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.Config;
import net.creeperhost.blockshot.capture.GifEncoder;
import net.creeperhost.blockshot.capture.RecordingHandler;
import net.creeperhost.blockshot.capture.ScreenshotHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.time.Instant;

/**
 * Created by brandon3055 on 18/03/2023
 */
public class GuiEvents {

    private static long keybindLast = 0;

    public static void init() {
        ClientRawInputEvent.KEY_PRESSED.register(GuiEvents::onRawInput);
        ClientGuiEvent.INIT_POST.register(GuiEvents::onGuiInit);
    }

    private static void onGuiInit(Screen screen, ScreenAccess access) {
        if (screen instanceof PauseScreen) {
            Config.ButtonPos pos = Config.INSTANCE.buttonPos;
            access.addRenderableWidget(new IconButton(pos.getX(screen.width, 100), pos.getY(screen.height, 20), 100, 20, Component.translatable("gui.blockshot.blockshot_button"), e -> Minecraft.getInstance().setScreen(new BlockShotHistoryScreen(screen)))
                    .setIcon(new ResourceLocation(BlockShot.MOD_ID, "textures/gui/blockshot_icon.png"), 16, 16)
            );
        }
    }

    private static EventResult onRawInput(Minecraft minecraft, int keyCode, int scanCode, int action, int modifiers) {
        if (!Minecraft.getInstance().options.keyScreenshot.matches(keyCode, scanCode) || action != 0) { //Have to use key release because key pressed does not get fired on forge for keyScreenshot.
            return EventResult.pass();
        }

        long elapsed = System.currentTimeMillis() - keybindLast;
        if (elapsed < 5000 && !RecordingHandler.getEncoder().isWorking()) {
            return EventResult.pass();
        }
        keybindLast = System.currentTimeMillis();

        if (Screen.hasControlDown()) {
            RecordingHandler.getEncoder().startOrStopRecording();
            return EventResult.interrupt(true);
        } else if (Screen.hasShiftDown()) {
            RecordingHandler.getEncoder().cancelRecording();
        }

        return EventResult.pass();
    }

    public static boolean handleComponentClick(Style style) {
        if (Screen.hasShiftDown() || style == null) return false;
        ClickEvent clickEvent = style.getClickEvent();
        if (!(clickEvent instanceof BlockShotClickEvent)) return false;

        //If we fail to upload here there is no need to write to disk because this image is already on disk.
        ScreenshotHandler.uploadLast(false);
        return true;
    }
}
