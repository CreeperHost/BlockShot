package net.creeperhost.blockshot.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.creeperhost.polylib.client.screen.widget.ScreenList;
import net.creeperhost.polylib.client.screen.widget.ScreenListEntry;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;

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
    public void render(PoseStack poseStack, int slotIndex, int y, int x, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float p_render_9_) {
        drawIcon(poseStack, x, y, 32, 32, getPreview());
        mc.font.draw(poseStack, DATE_FORMAT.format(capInfo.created * 1000L), x + 35, y, 0xffffff);
        mc.font.draw(poseStack, capInfo.getDisplay(), x + 35, y + 10, 0x808080);
    }

    private void drawIcon(PoseStack poseStack, int x, int y, int width, int height, ResourceLocation resourceLocation) {
        RenderSystem.enableBlend();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, resourceLocation);

        Matrix4f matrix4f = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, (float) x, (float) y + height, 0F).uv(0, 1).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x + width, (float) y + height, 0F).uv(1, 1).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x + width, (float) y, 0F).uv(1, 0).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x, (float) y, 0F).uv(0, 0).endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);

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
        if (!previewLoaded && !previewLoading && StringUtils.isNotBlank(capInfo.preview) && capInfo.created  > 0) {
            try {
                if (previewLoading) return new ResourceLocation("textures/misc/unknown_server.png");
                if (!previewLoaded) {
                    previewLoading = true;
                    NativeImage image = NativeImage.fromBase64(capInfo.preview);
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
