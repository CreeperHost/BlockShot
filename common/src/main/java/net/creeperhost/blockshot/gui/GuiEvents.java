package net.creeperhost.blockshot.gui;

import me.shedaniel.architectury.event.EventResult;
import me.shedaniel.architectury.event.events.GuiEvent;
import me.shedaniel.architectury.event.events.client.ClientRawInputEvent;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.Config;
import net.creeperhost.blockshot.capture.RecordingHandler;
import net.creeperhost.blockshot.capture.ScreenshotHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;

import java.util.List;

/**
 * Created by brandon3055 on 18/03/2023
 */
public class GuiEvents {

    private static long keybindLast = 0;

    public static void init() {
        ClientRawInputEvent.KEY_PRESSED.register(GuiEvents::onRawInput);
        GuiEvent.INIT_POST.register(GuiEvents::onGuiInit);
    }

    private static void onGuiInit(Screen screen, List<AbstractWidget> widgets, List<GuiEventListener> children) {
        if (screen instanceof PauseScreen) {
            Config.ButtonPos pos = Config.INSTANCE.buttonPos;
            IconButton button = new IconButton(pos.getX(screen.width, 100), pos.getY(screen.height, 20), 100, 20, new TranslatableComponent("gui.blockshot.blockshot_button"), e -> Minecraft.getInstance().setScreen(new BlockShotHistoryScreen(screen)))
                    .setIcon(new ResourceLocation(BlockShot.MOD_ID, "textures/gui/blockshot_icon.png"), 16, 16);
            children.add(button);
            widgets.add(button);
        }
    }

    private static InteractionResult onRawInput(Minecraft client, int keyCode, int scanCode, int action, int modifiers) {
        if (!client.options.keyScreenshot.matches(keyCode, scanCode) || action != 0) { //Have to use key release because key pressed does not get fired on forge for keyScreenshot.
            return InteractionResult.PASS;
        }

        long elapsed = System.currentTimeMillis() - keybindLast;
        if (elapsed < 5000 && !RecordingHandler.getEncoder().isWorking()) {
            return InteractionResult.PASS;
        }
        keybindLast = System.currentTimeMillis();

        if (Screen.hasControlDown()) {
            RecordingHandler.getEncoder().startOrStopRecording();
            return InteractionResult.CONSUME;
        } else if (Screen.hasShiftDown()) {
            RecordingHandler.getEncoder().cancelRecording();
        }

        return InteractionResult.PASS;
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
