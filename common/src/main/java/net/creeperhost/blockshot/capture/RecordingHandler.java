package net.creeperhost.blockshot.capture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.creeperhost.blockshot.Auth;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.List;

/**
 * Created by brandon3055 on 24/03/2023
 */
public class RecordingHandler {

    private static Encoder encoder;

    public static Encoder getEncoder() {
        if (encoder == null) {
            setEncoder(Config.INSTANCE.getEncoderType().createEncoder());
        }
        return encoder;
    }

    public static boolean setEncoder(Encoder newEncoder) {
        if (encoder != null && encoder.isWorking()) return false;
        encoder = newEncoder;
        return true;
    }

    public static void handleScreenCapture() {
        if (!BlockShot.isActive() || !getEncoder().isWorking()) {
            return;
        }

        getEncoder().updateCapture();

        drawRecordingIndicator(5, 5);
    }

    private static void drawRecordingIndicator(int x, int y) {
        List<Component> hudLines = getEncoder().getHudText();
        if (hudLines == null || hudLines.isEmpty()) return;

        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
        matrix4fStack.pushMatrix();
        matrix4fStack.translation(0.0F, 0.0F, -2000.0F);
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableBlend();
        Font font = Minecraft.getInstance().font;

        int recordOffset = getEncoder().showRecordIcon() ? 10 : 0;

        int maxWidth = 0;
        for (Component line : hudLines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }
        int height = (hudLines.size() * 9) + 5;

        drawRect(matrix4fStack, x, y, maxWidth + 6 + recordOffset, height, 0xb0101010);

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        int i = 0;
        for (Component line : hudLines) {
            font.drawInBatch(line, x + 3 + recordOffset, y + 3 + i, 0xFFFFFF, true, matrix4fStack, bufferSource, Font.DisplayMode.NORMAL, 0, 0xf000f0);
            i += 9;
        }
        bufferSource.endBatch();

        if (System.currentTimeMillis() % 2000 > 1000 && getEncoder().showRecordIcon()) {
            drawRect(matrix4fStack, x + 3, y + 4, 7, 5, 0xFFFF0000);
            drawRect(matrix4fStack, x + 4, y + 3, 5, 7, 0xFFFF0000);
        }

        matrix4fStack.popMatrix();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.disableBlend();
    }

    private static void drawRect(Matrix4fStack poseStack, int x, int y, int width, int height, int colour) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.addVertex(poseStack, x, y + height, 0).setColor(colour);
        bufferBuilder.addVertex(poseStack, x + width, y + height, 0).setColor(colour);
        bufferBuilder.addVertex(poseStack, x + width, y, 0).setColor(colour);
        bufferBuilder.addVertex(poseStack, x, y, 0).setColor(colour);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }
}
