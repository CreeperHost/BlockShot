package net.creeperhost.blockshot.gui;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.hooks.client.screen.ScreenAccess;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.Config;
import net.creeperhost.blockshot.GifEncoder;
import net.creeperhost.blockshot.ScreenshotHandler;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
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
        if (Screen.hasControlDown()) {
            if (Minecraft.getInstance().options.keyScreenshot.matches(keyCode, scanCode)) {
                if (keybindLast + 5 > Instant.now().getEpochSecond()) return EventResult.pass();
                keybindLast = Instant.now().getEpochSecond();
                if (GifEncoder.isRecording) {
                    GifEncoder.isRecording = false;
                } else if (GifEncoder.processedFrames.get() == 0 && GifEncoder.addedFrames.get() == 0) {
                    GifEncoder.begin();
                }
                return EventResult.interrupt(true);
            }
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
