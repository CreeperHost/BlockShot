package net.creeperhost.blockshot.gui;

import net.creeperhost.polylib.client.modulargui.lib.GuiRender;
import net.creeperhost.polylib.client.modulargui.sprite.Material;
import net.creeperhost.polylib.client.modulargui.sprite.PolyTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

/**
 * Created by brandon3055 on 19/03/2023
 */
public class IconButton extends Button {
    private final boolean showText;
    private Material icon;
    private int iconWidth;
    private int iconHeight;

    public IconButton(int x, int y, int width, int height, @Nullable Component component, OnPress onPress) {
        super(x, y, width, height, component == null ? Component.empty() : component, onPress, Button.DEFAULT_NARRATION);
        showText = component != null;
    }

    public IconButton setIcon(ResourceLocation icon, int iconWidth, int iconHeight) {
        this.icon = Material.fromRawTexture(icon);
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiRender render = new GuiRender(guiGraphics);

        Material buttonMat = PolyTextures.get(() -> this.isHoveredOrFocused() ? "dynamic/button_highlight" : "dynamic/button_vanilla");
        render.dynamicTex(buttonMat, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 4, 4, 4, 4, 0xFFFFFFFF);
        Minecraft minecraft = Minecraft.getInstance();

        int drawX = (getX() + width / 2);
        if (icon != null) {
            drawX -= (iconWidth / 2);
        }
        FormattedCharSequence formattedCharSequence = null;
        if (showText) {
            formattedCharSequence = getMessage().getVisualOrderText();
            drawX -= (minecraft.font.width(formattedCharSequence) / 2) + (icon == null ? 0 : 2);
        }

        if (icon != null) {
            render.texRect(icon, drawX, getY() + height / 2 - iconHeight / 2, 16, 16);
        }

        if (showText) {
            if (icon != null) {
                drawX += iconWidth + 2;
            }
            render.drawString(formattedCharSequence, drawX, (int) (getY() + (height - 8) / 2F), 0xFFFFFFFF, true);
        }
    }
}
