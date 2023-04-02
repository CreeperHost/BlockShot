package net.creeperhost.blockshot.capture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.creeperhost.blockshot.Auth;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Created by brandon3055 on 24/03/2023
 */
public class RecordingHandler {

    private static Encoder encoder;

    public static Encoder getEncoder() {
        if (Config.INSTANCE.getEncoderType().requiresPremium() && !Auth.hasPremium()) {
            Config.INSTANCE.setEncoderType(Config.EncoderType.GIF);
        }

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

        RenderSystem.enableBlend();
        PoseStack poseStack = new PoseStack();

        Font font = Minecraft.getInstance().font;

        int recordOffset = getEncoder().showRecordIcon() ? 10 : 0;

        int maxWidth = 0;
        for (Component line : hudLines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }
        int height = (hudLines.size() * 9) + 5;

        drawRect(poseStack, x, y, maxWidth + 6 + recordOffset, height, 0xb0101010);

        int i = 0;
        for (Component line : hudLines) {
            font.draw(poseStack, line, x + 3 + recordOffset, y + 3 + i, 0xFFFFFF);
            i += 9;
        }

        if (System.currentTimeMillis() % 2000 > 1000 && getEncoder().showRecordIcon()) {
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
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);
    }
}
