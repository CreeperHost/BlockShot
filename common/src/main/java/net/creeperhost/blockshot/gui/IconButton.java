package net.creeperhost.blockshot.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.creeperhost.polylib.client.modulargui.lib.GuiRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Created by brandon3055 on 19/03/2023
 */
public class IconButton extends Button {
    private static final WidgetSprites SPRITES = new WidgetSprites(new ResourceLocation("widget/button"), new ResourceLocation("widget/button_disabled"), new ResourceLocation("widget/button_highlighted"));
    private final boolean showText;
    private ResourceLocation icon;
    private int iconWidth;
    private int iconHeight;

    public IconButton(int x, int y, int width, int height, @Nullable Component component, OnPress onPress) {
        super(x, y, width, height, component == null ? Component.empty() : component, onPress, Button.DEFAULT_NARRATION);
        showText = component != null;
    }

    public IconButton setIcon(ResourceLocation icon, int iconWidth, int iconHeight) {
        this.icon = icon;
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
        return this;
    }

    private int getTextureY() {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (this.isHoveredOrFocused()) {
            i = 2;
        }

        return 46 + i * 20;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        guiGraphics.blitSprite(SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

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
            draw(icon, guiGraphics.pose(), drawX, getY() + height / 2 - iconHeight / 2, 16, 16);
        }

        if (showText) {
            if (icon != null) {
                drawX += iconWidth + 2;
            }
            guiGraphics.drawString(minecraft.font, formattedCharSequence, drawX, (int) (getY() + (height - 8) / 2F), 0xFFFFFF, true);
        }
    }

    private void draw(ResourceLocation resourceLocation, PoseStack poseStack, int x, int y, int width, int height) {
        RenderSystem.setShaderTexture(0, resourceLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, (float) x, (float) y + height, (float) 0).uv(0, 1).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x + width, (float) y + height, (float) 0).uv(1, 1).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x + width, (float) y, (float) 0).uv(1, 0).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x, (float) y, (float) 0).uv(0, 0).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }
}
