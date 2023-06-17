package net.creeperhost.blockshot.gui;

import com.google.common.base.Charsets;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.creeperhost.polylib.client.screen.widget.ScreenList;
import net.creeperhost.polylib.client.screen.widget.ScreenListEntry;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

import java.text.SimpleDateFormat;
import java.util.Base64;

/**
 * Created by brandon3055 on 19/03/2023
 */
public class BlockShotListEntry extends ScreenListEntry {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    public final ScreenCapInfo capInfo;

    public BlockShotListEntry(ScreenList list, ScreenCapInfo capInfo) {
        super(list);
        this.capInfo = capInfo;
    }


    @Override
    public void render(GuiGraphics graphics, int slotIndex, int y, int x, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float p_render_9_) {
        drawIcon(graphics, x, y, 32, 32, getPreview());
        graphics.drawString(mc.font, DATE_FORMAT.format(capInfo.created * 1000L), x + 35, y, 0xffffff);
        graphics.drawString(mc.font, capInfo.getDisplay(), x + 35, y + 10, 0x808080);
    }

    private void drawIcon(GuiGraphics graphics, int x, int y, int width, int height, ResourceLocation resourceLocation) {
        RenderSystem.enableBlend();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, resourceLocation);

        Matrix4f matrix4f = graphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, (float) x, (float) y + height, 0F).uv(0, 1).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x + width, (float) y + height, 0F).uv(1, 1).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x + width, (float) y, 0F).uv(1, 0).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x, (float) y, 0F).uv(0, 0).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.disableBlend();
    }

    public void copyUrl() {
        Minecraft.getInstance().keyboardHandler.setClipboard("https://blockshot.ch/" + capInfo.id);
    }

    public void openUrl() {
        Util.getPlatform().openUri("https://blockshot.ch/" + capInfo.id);
    }

    private boolean previewLoading = false;
    private boolean previewLoaded = false;
    private ResourceLocation _resource = new ResourceLocation("textures/misc/unknown_server.png");

    public ResourceLocation getPreview() {
        if (!previewLoaded && !previewLoading && StringUtils.isNotBlank(capInfo.preview) && capInfo.created > 0) {
            try {
                if (previewLoading) return new ResourceLocation("textures/misc/unknown_server.png");
                if (!previewLoaded) {
                    previewLoading = true;
                    byte[] bs = Base64.getDecoder().decode(capInfo.preview.replaceAll("\n", "").getBytes(Charsets.UTF_8));
                    NativeImage image = NativeImage.read(bs);
                    DynamicTexture i = new DynamicTexture(image);
                    _resource = Minecraft.getInstance().getTextureManager().register("blockshot/", i);
                    //i.close();
                    previewLoaded = true;
                    previewLoading = false;
                } else {
                    return _resource;
                }
            } catch (Throwable t) {
                LOGGER.warn("An error occurred while loading capture preview", t);
                previewLoading = false;
                previewLoaded = true;//Let's not retry...
            }
        }
        return _resource;
    }
}
