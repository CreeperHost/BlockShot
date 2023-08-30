package net.creeperhost.blockshot.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Supplier;

/**
 * Created by brandon3055 on 27/03/2023
 */
public class RadioButton extends Button {
    private Supplier<Boolean> selected;

    public RadioButton(int i, int j, int k, int l, Component component, OnPress onPress) {
        super(i, j, k, l, component, onPress, Button.DEFAULT_NARRATION);
    }

    public void setSelected(Supplier<Boolean> selected) {
        this.selected = selected;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        if (selected.get()) return;
        super.playDownSound(soundManager);
    }

    @Override
    public void onPress() {
        if (selected.get()) return;
        super.onPress();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean selected = this.selected.get();
        guiGraphics.setColor(selected ? 0.5F : 1F, 1F, selected ? 0.5F : 1F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        guiGraphics.blitNineSliced(WIDGETS_LOCATION, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0, this.getTextureY());
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int k = this.active ? 16777215 : 10526880;
        this.renderString(guiGraphics, minecraft.font, k | Mth.ceil(this.alpha * 255.0F) << 24);
    }

    @Override
    public void renderString(GuiGraphics guiGraphics, Font font, int i) {
        this.renderScrollingString(guiGraphics, font, 2, i);
    }

    private int getTextureY() {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (this.isHoveredOrFocused() && !selected.get()) {
            i = 2;
        }

        return 46 + i * 20;
    }
}
