package net.creeperhost.blockshot.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
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
        super(i, j, k, l, component, onPress);
    }

    public RadioButton(int i, int j, int k, int l, Component component, OnPress onPress, OnTooltip onTooltip) {
        super(i, j, k, l, component, onPress, onTooltip);
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
    public void renderButton(PoseStack poseStack, int i, int j, float f) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        boolean selected = this.selected.get();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
        RenderSystem.setShaderColor(selected ? 0.5F : 1F, 1F, selected ? 0.5F : 1F, this.alpha);
        int k = this.getYImage(this.isHoveredOrFocused() && !selected);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        this.blit(poseStack, this.x, this.y, 0, 46 + k * 20, this.width / 2, this.height);
        this.blit(poseStack, this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + k * 20, this.width / 2, this.height);
        this.renderBg(poseStack, minecraft, i, j);
        int l = this.active ? 16777215 : 10526880;
        drawCenteredString(poseStack, font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, l | Mth.ceil(this.alpha * 255.0F) << 24);

        if (this.isHoveredOrFocused()) {
            this.renderToolTip(poseStack, i, j);
        }
    }
}
