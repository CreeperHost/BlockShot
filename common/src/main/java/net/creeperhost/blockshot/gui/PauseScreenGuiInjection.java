package net.creeperhost.blockshot.gui;

import net.creeperhost.blockshot.Config;
import net.creeperhost.polylib.client.modulargui.ModularGui;
import net.creeperhost.polylib.client.modulargui.ModularGuiScreen;
import net.creeperhost.polylib.client.modulargui.elements.GuiButton;
import net.creeperhost.polylib.client.modulargui.elements.GuiElement;
import net.creeperhost.polylib.client.modulargui.elements.GuiText;
import net.creeperhost.polylib.client.modulargui.elements.GuiTexture;
import net.creeperhost.polylib.client.modulargui.lib.Constraints;
import net.creeperhost.polylib.client.modulargui.lib.GuiProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.creeperhost.polylib.client.modulargui.lib.geometry.Constraint.*;
import static net.creeperhost.polylib.client.modulargui.lib.geometry.GeoParam.*;

/**
 * Adds BlockShot's pause-menu entry through PolyLib's screen injection layer.
 */
public class PauseScreenGuiInjection implements GuiProvider {
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ICON_SIZE = 16;

    @Override
    public void buildGui(ModularGui gui) {
        gui.initFullscreenGui();
        GuiElement<?> root = gui.getRoot();

        GuiButton button = GuiButton.vanilla(root, null)
                .onClick(() -> Minecraft.getInstance().setScreen(new ModularGuiScreen(new BlockShotGui())));
        Constraints.size(button, BUTTON_WIDTH, BUTTON_HEIGHT);
        button.constrain(LEFT, dynamic(() -> (double) Config.INSTANCE.buttonPos.getX((int) root.xSize(), BUTTON_WIDTH)));
        button.constrain(TOP, dynamic(() -> (double) Config.INSTANCE.buttonPos.getY((int) root.ySize(), BUTTON_HEIGHT)));

        GuiTexture icon = new GuiTexture(button, ModTextures.get("blockshot_icon"))
                .constrain(WIDTH, literal(ICON_SIZE))
                .constrain(HEIGHT, literal(ICON_SIZE))
                .constrain(LEFT, relative(button.get(LEFT), 4))
                .constrain(TOP, midPoint(button.get(TOP), button.get(BOTTOM), ICON_SIZE / -2D));

        new GuiText(button, Component.translatable("gui.blockshot.blockshot_button"))
                .constrain(LEFT, relative(icon.get(RIGHT), 2))
                .constrain(RIGHT, relative(button.get(RIGHT), -4))
                .constrain(TOP, match(button.get(TOP)))
                .constrain(BOTTOM, match(button.get(BOTTOM)));
    }
}
