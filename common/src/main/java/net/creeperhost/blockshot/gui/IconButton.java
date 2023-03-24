package net.creeperhost.blockshot.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

/**
 * Created by brandon3055 on 19/03/2023
 */
public class IconButton extends Button {
    private final boolean showText;
    private ResourceLocation icon;
    private int iconWidth;
    private int iconHeight;

    public IconButton(int x, int y, int width, int height, @Nullable Component component, OnPress onPress) {
        super(x, y, width, height, component == null ? Component.empty() : component, onPress);
        showText = component != null;
    }

    public IconButton(int x, int y, int width, int height, @Nullable Component component, OnPress onPress, OnTooltip onTooltip) {
        super(x, y, width, height, component == null ? Component.empty() : component, onPress, onTooltip);
        showText = component != null;
    }

    public IconButton setIcon(ResourceLocation icon, int iconWidth, int iconHeight) {
        this.icon = icon;
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
        return this;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        int k = getYImage(isHoveredOrFocused());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        blit(poseStack, x, y, 0, 46 + k * 20, width / 2, height);
        blit(poseStack, x + width / 2, y, 200 - width / 2, 46 + k * 20, width / 2, height);

        int drawX = (x + width / 2);
        if (icon != null) {
            drawX -= (iconWidth / 2);
        }
        FormattedCharSequence formattedCharSequence = null;
        if (showText) {
            formattedCharSequence = getMessage().getVisualOrderText();
            drawX -= (font.width(formattedCharSequence) / 2) + (icon == null ? 0 : 2);
        }

        if (icon != null) {
            RenderSystem.setShaderTexture(0, icon);
            draw(poseStack, drawX, y + height / 2 - iconHeight / 2, 16, 16);
        }

        if (showText) {
            if (icon != null) {
                drawX += iconWidth + 2;
            }
            font.drawShadow(poseStack, formattedCharSequence, drawX, y + (height - 8) / 2F, 0xFFFFFF);
        }

        if (isHoveredOrFocused()) {
            renderToolTip(poseStack, mouseX, mouseY);
        }
    }

    private void draw(PoseStack poseStack, int x, int y, int width, int height) {
        Matrix4f matrix4f = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, (float) x, (float) y + height, (float) getBlitOffset()).uv(0, 1).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x + width, (float) y + height, (float) getBlitOffset()).uv(1, 1).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x + width, (float) y, (float) getBlitOffset()).uv(1, 0).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x, (float) y, (float) getBlitOffset()).uv(0, 0).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }
}
