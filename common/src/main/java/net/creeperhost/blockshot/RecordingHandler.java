package net.creeperhost.blockshot;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;

/**
 * Created by brandon3055 on 24/03/2023
 */
public class RecordingHandler {

    public static void handleScreenCapture() {
        if (!GifEncoder.isRecording || !BlockShot.isActive()) {
            return;
        }

        int skipFrames = 6;
        if (BlockShot.getFPS() > 20) {
            skipFrames = (BlockShot.getFPS() / 10);
        }

        if (GifEncoder.frames > skipFrames || (GifEncoder.lastTimestamp != (System.currentTimeMillis() / 1000))) {
            GifEncoder.frames = 0;
            if (GifEncoder.lastTimestamp != (System.currentTimeMillis() / 1000)) {
                GifEncoder.lastTimestamp = (System.currentTimeMillis() / 1000);
                GifEncoder.totalSeconds++;
            }
            RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
            NativeImage nativeImage = new NativeImage(renderTarget.width, renderTarget.height, false);
            RenderSystem.bindTexture(renderTarget.getColorTextureId());
            nativeImage.downloadTexture(0, true);
            GifEncoder.addFrameAndClose(nativeImage);
            if (GifEncoder.totalSeconds > 30) GifEncoder.isRecording = false;
        } else {
            GifEncoder.frames++;
        }

        drawRecordingIndicator(5, 5);
    }

    private static void drawRecordingIndicator(int x, int y) {
        RenderSystem.enableBlend();
        PoseStack poseStack = new PoseStack();

        String screenshotKey = Minecraft.getInstance().options.keyScreenshot.getTranslatedKeyMessage().getString();
        String recording = I18n.get("overlay.blockshot.recording");
        String finish = I18n.get("overlay.blockshot.finish", screenshotKey);
        String cancel = I18n.get("overlay.blockshot.cancel", screenshotKey);

        Font font = Minecraft.getInstance().font;
        int maxWidth = Math.max(font.width(recording) + 6, Math.max(font.width(finish), font.width(cancel)));

        drawRect(poseStack, x, y, maxWidth + 6, 27 + 6, 0xb0101010);

        font.draw(poseStack, ChatFormatting.RED + recording, x + 3 + 10, y + 3, 0xFFFFFF);
        font.draw(poseStack, ChatFormatting.GRAY + finish, x + 3, y + 3 + 9, 0xFFFFFF);
        font.draw(poseStack, ChatFormatting.GRAY + cancel, x + 3, y + 3 + 18, 0xFFFFFF);

        if (System.currentTimeMillis() % 2000 > 1000) {
            drawRect(poseStack, x + 3, y + 4, 7, 5, 0xFFFF0000);
            drawRect(poseStack, x + 4, y + 3, 5, 7, 0xFFFF0000);
        }

        RenderSystem.disableBlend();
    }

    private static void drawRect(PoseStack poseStack, int x, int y, int width, int height, int colour) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix4f = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, x, y + height, 0).color(colour).endVertex();
        bufferBuilder.vertex(matrix4f, x + width, y + height, 0).color(colour).endVertex();
        bufferBuilder.vertex(matrix4f, x + width, y, 0).color(colour).endVertex();
        bufferBuilder.vertex(matrix4f, x, y, 0).color(colour).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }
}
